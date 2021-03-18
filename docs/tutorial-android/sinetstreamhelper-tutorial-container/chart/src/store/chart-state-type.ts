export type TypeChart = 'line' | 'bar';

export interface ChartOption {
  id: number;
  title: string;
  chart: TypeChart;
  sensor: string;
  color?: string;
}
