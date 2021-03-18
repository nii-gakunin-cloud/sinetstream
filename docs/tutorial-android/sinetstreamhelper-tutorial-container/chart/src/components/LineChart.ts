import Chart, { ChartOptions } from 'chart.js';
import { Line, mixins as chartMixins } from 'vue-chartjs';
import { mixins } from 'vue-class-component';
import { Component, Prop } from 'vue-property-decorator';

const { reactiveProp } = chartMixins;

@Component
export default class LineChart extends mixins(Line, reactiveProp) {
  @Prop() readonly options?: ChartOptions;

  mounted() {
    Object.defineProperty(Chart.defaults.global.animation, 'duration', {
      value: 0,
    });
    this.renderChart(this.chartData, this.options);
  }
}
