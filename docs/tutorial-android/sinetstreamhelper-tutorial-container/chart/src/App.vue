<template>
  <v-app>
    <v-app-bar
      app
      color="blue-grey lighten-4"
    >
      <v-img
        src="@/assets/logo.png"
        alt="logo"
        class="shrink mr-2"
        contain
        width="48"
      />
      <v-toolbar-title>SINETStream</v-toolbar-title>
      <v-spacer />
      <chart-dialog />
      <settings-dialog />
    </v-app-bar>

    <v-main>
      <sensor-chart />
    </v-main>
  </v-app>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import ChartDialog from './components/ChartDialog.vue';
import SettingsDialog from './components/SettingsDialog.vue';
import SensorChart from './components/SensorChart.vue';

@Component({
  components: {
    ChartDialog,
    SettingsDialog,
    SensorChart,
  },
})
export default class App extends Vue {
}
</script>
