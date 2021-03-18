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
import { Component, Prop, Vue } from 'vue-property-decorator';
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

  @Prop({ type: Number, default: () => 3000 }) readonly refresh!: number;

  updateData() {
    const url = document.location.origin;
    const { sensors } = this.$store.getters;
    this.$store.dispatch('updateData', { url, sensors });
  }

  updateTimer() {
    if (Object.values(this.$store.state.options).length > 0) {
      if (!this.timerId) {
        this.timerId = setInterval(() => {
          this.updateData();
        }, this.refresh);
      }
    } else if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = undefined;
    }
  }
}
</script>
