<template>
  <v-card
    elevation="2"
    class="ma-3"
  >
    <v-toolbar
      class="mb-4"
      color="accent"
      dense
    >
      <v-toolbar-title>
        {{ options.title }}
      </v-toolbar-title>
      <v-spacer />
      <v-btn
        icon
        @click="close"
      >
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </v-toolbar>
    <bar-chart
      v-if="options.chart === 'bar'"
      key="bar-chart"
      :chart-data="dataset"
      :options="chartJsOptions"
    />
    <line-chart
      v-else
      key="line-chart"
      :chart-data="dataset"
      :options="chartJsOptions"
    />
  </v-card>
</template>

<script lang="ts">
import { ChartOption } from '@/store';
import { ChartData } from 'chart.js';
import {
  Component, Emit, Prop, Vue,
} from 'vue-property-decorator';
import BarChart from './BarChart';
import LineChart from './LineChart';

const chartJsOptions = {
  responsive: true,
  maintainAspectRatio: false,
  legend: {
    display: false,
  },
  scales: {
    xAxes: [
      {
        type: 'time',
        time: { unit: 'minute' },
      },
    ],
    yAxes: [
      {
        ticks: { beginAtZero: true },
      },
    ],
  },
};

@Component({
  components: {
    LineChart,
    BarChart,
  },
})
export default class SensorChartCard extends Vue {
  chartJsOptions = chartJsOptions;

  @Prop({ type: Number, required: true }) readonly dataId!: number;

  get dataset(): ChartData {
    const ret = this.$store.state.dataset[this.dataId];
    if (!ret) {
      return this.$store.getters.emptyDataset;
    }
    return ret;
  }

  get options(): ChartOption {
    return this.$store.state.options[this.dataId];
  }

  @Emit('del-chart')
  close() {
    this.$store.dispatch('deleteDataset', { id: this.dataId });
  }

  @Emit('add-chart')
  // eslint-disable-next-line @typescript-eslint/no-empty-function, class-methods-use-this
  mounted() {
  }
}
</script>
