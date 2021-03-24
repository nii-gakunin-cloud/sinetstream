<template>
  <v-dialog
    v-model="show"
    max-width="400"
  >
    <template #activator="{ on, attrs }">
      <v-btn
        class="mx-2"
        fab
        small
        color="primary"
        v-bind="attrs"
        v-on="on"
      >
        <v-icon>mdi-plus</v-icon>
      </v-btn>
    </template>

    <v-card>
      <v-card-title class="headline info lighten-2">
        グラフの追加
      </v-card-title>
      <v-card-text>
        <v-form
          ref="form"
          v-model="valid"
          lazy-validation
        >
          <v-text-field
            v-model="sensor"
            label="センサータイプ"
            required
            :rules="rules"
          />
          <v-text-field
            v-model="title"
            label="タイトル"
          />
          <v-select
            v-model="chart"
            :items="chartTypes"
            label="グラフ種別"
            :rules="rules"
            required
          />

          <v-select
            v-if="!showPalette"
            v-model="colorText"
            :items="colorList"
            label="表示色"
            append-outer-icon="mdi-palette"
            @click:append-outer="switchColorPallete"
          />

          <template v-if="showPalette">
            <div>表示色</div>

            <v-color-picker
              v-model="color"
              :swatches="swatches"
              show-swatches
              hide-canvas
              hide-inputs
            />
          </template>
        </v-form>
      </v-card-text>
      <v-divider />
      <v-card-actions>
        <v-spacer />
        <v-btn
          color="primary"
          :disabled="!valid"
          @click="addChart"
        >
          追加
        </v-btn>
        <v-btn
          color="secondary"
          @click="closeDialog"
        >
          閉じる
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import colors from 'vuetify/lib/util/colors';

interface VForm {
  validate: () => boolean;
  reset: () => void;
  resetValidation: () => void;
}

@Component
export default class ChartDialog extends Vue {
  show = false;

  showPalette = false;

  valid = true;

  sensor = '';

  title = '';

  chart = 'line';

  chartTypes = [
    {
      text: '折れ線グラフ',
      value: 'line',
    },
    {
      text: '棒グラフ',
      value: 'bar',
    },
  ]

  alpha = 'a0';

  rules = [(v: string) => !!v || 'required'];

  color?: string = `${colors.red.base}${this.alpha}`;

  colorList = [
    {
      text: '赤',
      value: colors.red.base,
    },
    {
      text: '黄',
      value: colors.yellow.base,
    },
    {
      text: '緑',
      value: colors.green.base,
    },
    {
      text: 'シアン',
      value: colors.cyan.base,
    },
    {
      text: '藍色',
      value: colors.indigo.base,
    },
    {
      text: 'ピンク',
      value: colors.pink.base,
    },
    {
      text: 'ライム',
      value: colors.lime.base,
    },
    {
      text: 'ライトグリーン',
      value: colors.lightGreen.base,
    },
    {
      text: 'ライトブルー',
      value: colors.lightBlue.base,
    },
    {
      text: '青',
      value: colors.blue.base,
    },
  ];

  swatches = [
    [colors.red.base, colors.pink.base],
    [colors.yellow.base, colors.lime.base],
    [colors.green.base, colors.lightGreen.base],
    [colors.cyan.base, colors.lightBlue.base],
    [colors.indigo.base, colors.blue.base],
  ].map((l) => l.map((c) => `${c}${this.alpha}`));

  closeDialog() {
    this.show = false;
  }

  validate(): boolean {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (this.$refs.form as any).validate();
  }

  addChart() {
    if (this.validate()) {
      this.closeDialog();
      if (this.title.length === 0) {
        this.title = this.sensor;
      }
      const {
        sensor, title, chart, color,
      } = this;
      this.$store.dispatch('appendDataset', {
        sensor, title, chart, color,
      });
    }
  }

  get colorText() {
    const current = this.color;
    if (!current) {
      return '';
    }
    const color = this.colorList.find(
      (x) => current.toUpperCase().startsWith(x.value.toUpperCase()),
    )?.value;
    return color || '';
  }

  set colorText(value: string) {
    const color = this.colorList.find(
      (x) => value.toUpperCase().startsWith(x.value.toUpperCase()),
    )?.value;
    if (color) {
      this.color = `${color}${this.alpha}`;
    }
  }

  switchColorPallete() {
    this.showPalette = true;
  }
}
</script>
