<template>
  <div class="chart-card">
    <sensor-chart-card
      v-for="item in options"
      :key="item.id"
      :data-id="item.id"
      @del-chart="updateTimer"
      @add-chart="updateTimer"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { mapState } from 'vuex';
import SensorChartCard from './SensorChartCard.vue';

@Component({
  computed: mapState(['options']),
  components: {
    SensorChartCard,
  },
})
export default class SensorChart extends Vue {
  timerId?: number;

  updateData() {
    const url = document.location.origin;
    const { sensors } = this.$store.getters;
    this.$store.dispatch('updateData', { url, sensors });
  }

  updateTimer() {
    const refresh = this.$store.state.refreshInterval * 1000;
    if (Object.values(this.$store.state.options).length > 0) {
      if (this.timerId) {
        clearInterval(this.timerId);
      }
      this.timerId = setInterval(() => {
        this.updateData();
      }, refresh);
    } else if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }

  mounted() {
    this.$store.subscribe((mutation) => {
      if (mutation.type === 'updateSettings') {
        this.updateTimer();
      }
    });
  }
}
</script>
