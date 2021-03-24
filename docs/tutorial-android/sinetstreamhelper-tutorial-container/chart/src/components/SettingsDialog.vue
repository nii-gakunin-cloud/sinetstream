<template>
  <v-dialog
    v-model="show"
    max-width="400"
  >
    <template #activator="{ on, attrs }">
      <v-btn
        class="mx-2"
        icon
        small
        v-bind="attrs"
        v-on="on"
      >
        <v-icon>mdi-cog</v-icon>
      </v-btn>
    </template>

    <v-form @submit.prevent="updateSettings">
      <v-card>
        <v-card-title class="headline info lighten-2">
          設定
        </v-card-title>
        <v-card-text>
          <v-container>
            <v-row>
              <v-col>
                <v-text-field
                  v-model="refreshInterval"
                  label="更新間隔(秒)"
                  type="number"
                  min="1"
                />
              </v-col>
            </v-row>

            <v-row>
              <v-col>
                <v-text-field
                  v-model="timeRange.range"
                  label="最大表示範囲(分)"
                  hint="何分前のデータまで表示するかを指定する"
                  type="number"
                  min="1"
                  :disabled="timeRange.all"
                />
              </v-col>
              <v-col>
                <v-checkbox
                  v-model="timeRange.all"
                  label="全て"
                />
              </v-col>
            </v-row>

            <v-row>
              <v-col>
                <v-text-field
                  v-model="maxDataCount"
                  label="取得するデータ数"
                  hint="データ数が多すぎると更新が間に合わずグラフ表示が行われない場合があります"
                  type="number"
                  min="1"
                />
              </v-col>
            </v-row>
          </v-container>
        </v-card-text>

        <v-card-actions>
          <v-spacer />
          <v-btn
            color="primary"
            type="submit"
          >
            決定
          </v-btn>
          <v-btn
            color="secondary"
            @click="closeDialog"
          >
            閉じる
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-form>
  </v-dialog>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component
export default class SettingsDialog extends Vue {
  show = false;

  timeRange = {
    range: (this.$store.state.displayMaxMinutes > 0
      ? this.$store.state.displayMaxMinutes
      : 60),
    all: this.$store.state.displayMaxMinutes <= 0,
  };

  maxDataCount = this.$store.state.maxDataCount;

  refreshInterval = this.$store.state.refreshInterval;

  closeDialog() {
    this.show = false;
  }

  updateSettings() {
    const displayMaxMinutes = this.timeRange.all ? -1 : this.timeRange.range;
    const { maxDataCount, refreshInterval } = this;
    const settings = {
      displayMaxMinutes,
      maxDataCount,
      refreshInterval,
    };
    this.$store.dispatch('updateSettings', settings);
    this.show = false;
  }
}
</script>
