import { Store } from 'vuex';
import { ChartData } from 'chart.js';
import { TypeChart, ChartOption } from './chart-state-type';

interface ChartState {
  options: { [key: number]: ChartOption };
  dataset: { [key: number]: ChartData };
  init: boolean;
}

function generateChartOption(
  {
    id, sensor, chart = 'line', title = sensor, color,
  }: {
    id: number;
    sensor: string;
    chart?: TypeChart;
    title?: string;
    color?: string;
  },
): ChartOption {
  return {
    id,
    title,
    chart,
    sensor,
    color,
  };
}

const initOpts: { [key: number]: ChartOption } = {
  1: generateChartOption({
    id: 1,
    sensor: 'step_counter',
    title: '歩数',
    chart: 'bar',
    color: '#a6cee3',
  }),
  2: generateChartOption({
    id: 2,
    sensor: 'light',
    title: '照度',
    color: '#1b9e77a0',
  }),
  3: generateChartOption({
    id: 3,
    sensor: 'accelerometer',
    title: '加速度',
  }),
};

export default function initChartOptions() {
  return (store: Store<ChartState>) => {
    const { state } = store;
    if (state.init && Object.values(state.options).length === 0) {
      store.commit('init', { options: initOpts });
    }
  };
}
