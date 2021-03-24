import { DataPoint, fetchSensorData } from '@/api/sensor';
import { ChartData } from 'chart.js';
import Vue from 'vue';
import Vuex from 'vuex';
import createPersistedState from 'vuex-persistedstate';
import { ChartOption } from './chart-state-type';
import initChartOptions from './init-data';

Vue.use(Vuex);

export { ChartOption };

function generateChartData({ title, color }: { title?: string; color?: string }): ChartData {
  return {
    labels: [],
    datasets: [{
      label: title,
      data: [],
      backgroundColor: color,
    }],
  };
}

function nextId(options: { [key: number]: ChartOption }): number {
  if (Object.values(options).length === 0) {
    return 1;
  }
  return Math.max(...Object.values(options).map((x) => x.id)) + 1;
}

export default new Vuex.Store({
  state: {
    options: {} as { [key: number]: ChartOption },
    dataset: {} as { [key: number]: ChartData },
    init: true,
    displayMaxMinutes: 60,
    maxDataCount: 100,
    refreshInterval: 3,
  },
  getters: {
    sensors: (state) => {
      const sensors = Object.values(state.options).map((x) => x.sensor);
      return new Set(sensors);
    },
    ids: (state) => (sensor: string) => Object.values(state.options)
      .filter((x) => x.sensor === sensor)
      .map((x) => x.id),
    emptyDataset: () => generateChartData({}),
  },
  mutations: {
    appendDataset: (state, payload) => {
      const id = nextId(state.options);
      const opts = { id, ...payload };
      Vue.set(state.dataset, id, generateChartData(opts));
      Vue.set(state.options, id, opts);
      state.init = false;
    },
    deleteDataset: (state, { id }) => {
      Vue.delete(state.dataset, id);
      Vue.delete(state.options, id);
      state.init = false;
    },
    init: (state, payload) => {
      const { options } = payload;
      (Object.values(options) as ChartOption[]).forEach((opt) => {
        const dataset = generateChartData(opt);
        Vue.set(state.dataset, opt.id, dataset);
      });
      state.options = options;
    },
    updateData: (state, payload) => {
      const { id, datapoints } = payload;
      const { title, color } = state.options[id];
      const ds = {
        labels: datapoints.map((v: DataPoint) => v.x),
        datasets: [{
          label: title,
          backgroundColor: color,
          data: datapoints.map((v: DataPoint) => v.y),
          lineTension: 0,
        }],
      };
      Vue.set(state.dataset, id, ds);
    },
    updateSettings: (state, payload) => {
      state.displayMaxMinutes = payload.displayMaxMinutes;
      state.maxDataCount = payload.maxDataCount;
      state.refreshInterval = payload.refreshInterval;
    },
  },
  actions: {
    appendDataset({ commit }, payload) {
      commit('appendDataset', payload);
    },
    deleteDataset({ commit }, payload) {
      commit('deleteDataset', payload);
    },
    async updateData({ commit, getters, state }, { url, sensors }) {
      const { displayMaxMinutes, maxDataCount } = state;
      const data = await fetchSensorData({
        url, sensors, displayMaxMinutes, maxItems: maxDataCount,
      });
      data.forEach(({ sensor, datapoints }) => {
        getters.ids(sensor).forEach((id: number) => {
          commit('updateData', { id, datapoints });
        });
      });
    },
    async updateSettings({ commit }, payload) {
      commit('updateSettings', payload);
    },
  },
  modules: {
  },
  plugins: [
    createPersistedState({
      paths: [
        'options', 'init', 'displayMaxMinutes',
        'maxDataCount', 'refreshInterval',
      ],
    }),
    initChartOptions(),
  ],
});
