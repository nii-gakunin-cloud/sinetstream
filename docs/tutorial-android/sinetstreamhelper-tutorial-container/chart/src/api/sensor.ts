import S3, { Object as S3Object, ObjectKey } from 'aws-sdk/clients/s3';
import { minTime, parseISO, subMinutes } from 'date-fns';
import pLimit from 'p-limit';

interface SensorDataset {
  sensors: SensorData[];
  device: object;
}

interface SensorData {
  name: string;
  type: string;
  timestamp: string;
  value?: number;
  values?: number[];
}

interface SensorDataPoint {
  sensor: string;
  x: string;
  y: number;
}
export interface DataPoint {
  x: string;
  y: number;
}

async function getS3ObjectList(
  {
    s3, bucket = 'sensor-data', maxItems = -1,
  }: { s3: S3; bucket?: string; maxItems?: number },
) {
  const itemList = [];
  const params = { Bucket: bucket };
  let truncated = true;
  while (truncated) {
    // eslint-disable-next-line no-await-in-loop
    const data = await s3.makeUnauthenticatedRequest('listObjectsV2', params).promise();
    itemList.push(data.Contents.map((x: S3Object) => x.Key));
    truncated = data.IsTruncated;
    if (data.NextContinuationToken) {
      Object.defineProperty(params, 'ContinuationToken', {
        value: data.NextContinuationToken,
        configurable: true,
      });
    }
  }
  const items = itemList.flat();
  if (maxItems > 0 && items.length > maxItems) {
    items.splice(0, items.length - maxItems);
  }
  return items;
}

function toScalar(data: SensorData) {
  if (data.value) {
    return data.value;
  } if (data.values) {
    return Math.sqrt(data.values.reduce((r, a) => r + (a * a), 0));
  }
  return 0;
}

async function getSensorData(
  {
    s3, items, sensors, bucket = 'sensor-data', concurrency = 10,
  }: { s3: S3; items: ObjectKey[]; sensors: Set<string>; bucket?: string; concurrency?: number },
) {
  const limit = pLimit(concurrency);
  return Promise.all(
    items.map((item) => limit(async () => {
      const params = {
        Bucket: bucket,
        Key: item,
      };
      const ret = await s3.makeUnauthenticatedRequest('getObject', params).promise();
      const ds: SensorDataset = JSON.parse(ret.Body);
      return ds.sensors
        .filter((v) => sensors.has(v.type))
        .map((v) => ({
          sensor: v.type,
          x: v.timestamp,
          y: toScalar(v),
        }));
    })),
  );
}

export async function fetchSensorData({
  url,
  sensors,
  displayMaxMinutes = -1,
  maxItems = -1,
}: { url: string; sensors: Set<string>; displayMaxMinutes?: number; maxItems?: number }) {
  const s3 = new S3({
    endpoint: url,
    region: 'us-east-1',
    s3ForcePathStyle: true,
    s3BucketEndpoint: true,
  });
  const items = await getS3ObjectList({ s3, maxItems });
  const sensorsData: SensorDataPoint[][] = await getSensorData({ s3, items, sensors });
  const limit = displayMaxMinutes > 0
    ? subMinutes(Date.now(), displayMaxMinutes)
    : minTime;
  return Array.from(sensors).map((sensor) => ({
    sensor,
    datapoints: sensorsData.flat()
      .filter((v) => v.sensor === sensor)
      .filter((v) => displayMaxMinutes <= 0 || parseISO(v.x) >= limit)
      .map((v) => ({
        x: v.x,
        y: v.y,
      }))
      .sort((a, b) => a.x.localeCompare(b.x)),
  }));
}
