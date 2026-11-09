<template>
  <view class="guide-page">
    <view class="tabs"
      ><button
        class="plans-tab"
        :class="{ active: tab === 'plans' }"
        @tap="tab = 'plans'"
      >
        训练计划</button
      ><button
        class="exercises-tab"
        :class="{ active: tab === 'exercises' }"
        @tap="tab = 'exercises'"
      >
        动作指南
      </button></view
    >
    <view v-if="tab === 'plans'">
      <view v-for="plan in plans" :key="plan.key" class="plan-card">
        <view class="plan-cover"
          ><image :src="plan.image" mode="aspectFill" /><view class="plan-cover-content"
            ><view class="plan-title">{{ plan.name }}</view
            ><view class="plan-subtitle">{{ plan.subtitle }}</view></view
          ></view
        >
        <view class="plan-content"
          ><view class="plan-facts"
            ><text>{{ plan.cycle }}</text
            ><text>免费模板</text></view
          ><view class="plan-description">{{ plan.description }}</view>
          <view class="days"
            ><view
              v-for="(day, i) in plan.days"
              :key="i"
              :class="['day', { rest: day === '休息' }]"
              ><text class="day-index">{{ i + 1 }}</text
              ><text>{{ day }}</text></view
            ></view
          >
          <button class="plan-start" @tap="start(plan.key)">
            用这份计划开始 <text>→</text>
          </button>
        </view>
      </view></view>
    <view v-else>
      <view class="search-box"
        ><text>⌕</text
        ><input
          v-model="query"
          placeholder="搜索动作、肌群或器械"
          placeholder-class="placeholder"
        /><button v-if="query" @tap="query = ''">×</button></view
      >
      <scroll-view scroll-x class="muscle-scroll"
        ><view class="filter-list"
          ><button
            v-for="m in muscles"
            :key="m"
            :class="{ selected: muscle === m }"
            @tap="muscle = m"
          >
            {{ m }}
          </button></view
        ></scroll-view
      >
      <view class="library-meta"
        ><text>{{ exercises.length }} 个动作</text
        ><picker
          :range="equipmentOptions"
          :value="equipmentOptions.indexOf(equipment)"
          @change="equipment = equipmentOptions[$event.detail.value]"
          ><view class="equipment-filter"
            >{{ equipment === "全部" ? "全部器械" : equipment }} ▾</view
          ></picker
        ></view
      >
      <view
        v-for="exercise in exercises"
        :key="exercise.key"
        class="exercise-card"
        @tap="selected = exercise"
        ><view class="exercise-mark">{{ exercise.muscle.slice(0, 1) }}</view
        ><view class="exercise-body"
          ><view class="exercise-name">{{ exercise.name }}</view
          ><view class="exercise-meta"
            >{{ exercise.muscle }} / {{ exercise.equipment }}</view
          ><view class="exercise-summary">{{ exercise.summary }}</view></view
        ><text class="exercise-arrow">↗</text></view
      >
      <view v-if="!exercises.length" class="no-results"
        ><view>没有找到这个动作</view
        ><text>换个名称，或者试试其他肌群与器械。</text
        ><button
          @tap="
            query = '';
            muscle = '全部';
            equipment = '全部';
          "
        >
          清空筛选
        </button></view
      >
    </view>
    <view class="guide-bottom"
      ><button @tap="start()">定制我的计划 →</button></view
    >
    <view v-if="selected" class="detail-mask" @tap="selected = null"
      ><view class="detail-sheet" @tap.stop
        ><view class="detail-heading"
          ><view
            ><view class="eyebrow"
              >{{ selected.muscle }} / {{ selected.equipment }}</view
            ><view class="detail-title">{{ selected.name }}</view></view
          ><button @tap="selected = null">×</button></view
        ><scroll-view scroll-y class="detail-scroll"
          ><view class="detail-summary">{{ selected.summary }}</view
          ><view class="step" v-for="(step, i) in selected.steps" :key="i"
            ><text>{{ String(i + 1).padStart(2, "0") }}</text
            ><view>{{ step }}</view></view
          ><view class="cues"
            ><view v-for="cue in selected.cues" :key="cue"
              >· {{ cue }}</view
            ></view
          ></scroll-view
        ><button class="detail-cta" @tap="start()">添加到训练计划</button></view
      ></view
    >
  </view>
</template>
<script>
import {
  MUSCLE_FILTERS,
  EQUIPMENT_FILTERS,
  searchExercises,
} from "../../utils/exercise-library";
function resolveGuideImage(fileName) {
  let base = "/";
  // #ifdef H5
  base = import.meta.env.BASE_URL;
  // #endif
  return base + "static/images/" + fileName;
}

export default {
  data() {
    return {
      tab: "plans",
      query: "",
      muscle: "全部",
      equipment: "全部",
      selected: null,
      muscles: MUSCLE_FILTERS,
      equipmentOptions: EQUIPMENT_FILTERS,
      plans: [
        {
          key: "three-split",

          name: "三分化训练",
          subtitle: "推 · 拉 · 腿",
          cycle: "4 天一循环",
          image: resolveGuideImage("live-2.jpg"),
          description: "把推、拉和腿部分开安排，三次训练后留一天休息。",
          days: ["推", "拉", "腿", "休息"],
        },
        {
          key: "five-split",

          name: "五分化训练",
          subtitle: "胸 · 背 · 腿 · 肩 · 手臂",
          cycle: "7 天一循环",
          image: resolveGuideImage("live-3.jpg"),
          description: "每个训练日专注一个部位，为动作安排和休息留出空间。",
          days: ["胸", "背", "腿", "肩", "手臂", "休息", "休息"],
        },
      ],
    };
  },
  computed: {
    exercises() {
      return searchExercises(this.query, this.muscle, this.equipment);
    },
  },
  methods: {
    start(template) {
      uni.navigateTo({
        url:
          "/pages/training/index" + (template ? "?template=" + template : ""),
      });
    },
  },
};
</script>
<style scoped>
.guide-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 28rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
  color: var(--t1-text);
}
.eyebrow {
  font-size: 19rpx;
  letter-spacing: 3rpx;
  color: var(--t1-accent);
}
.tabs {
  display: flex;
  margin: 32rpx 0 26rpx;
  border-bottom: 1px solid var(--t1-border);
  gap: 32rpx;
}
.tabs button {
  margin: 0;
  padding: 16rpx 0 20rpx;
  color: var(--t1-muted);
  background: transparent;
  font-size: 28rpx;
  line-height: 1.5;
  border-radius: 0;
}
.tabs button.active {
  color: var(--t1-accent);
  border-bottom: 3rpx solid var(--t1-accent);
  font-weight: 700;
}
.tabs button text {
  font-size: 19rpx;
  color: var(--t1-muted);
  margin-left: 8rpx;
}
.plan-card {
  border: 1px solid var(--t1-border);
  border-radius: 22rpx;
  overflow: hidden;
  margin-bottom: 26rpx;
  background: var(--t1-surface);
}
.plan-cover {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 24rpx 24rpx 0;
}

.plan-cover image {
  width: 138rpx;
  height: 108rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
}

.plan-cover-content {
  min-width: 0;
}
.plan-title {
  font-size: 36rpx;
  font-weight: 650;
  line-height: 1.4;
}
.plan-subtitle {
  margin-top: 4rpx;
  color: var(--t1-muted);
  font-size: 24rpx;
}
.plan-content {
  padding: 24rpx;
}
.plan-facts {
  display: flex;
  gap: 18rpx;
  color: var(--t1-muted);
  font-size: 21rpx;
}
.plan-facts text + text {
  border-left: 1px solid var(--t1-border);
  padding-left: 18rpx;
}
.plan-description {
  font-size: 25rpx;
  color: var(--t1-muted);
  line-height: 1.8;
  margin: 20rpx 0;
}
.days {
  display: flex;
  gap: 8rpx;
}
.day {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 12rpx 2rpx;
  background: var(--t1-inset);
  border-radius: 10rpx;
  font-size: 21rpx;
  color: var(--t1-muted);
}
.day.rest {
  background: var(--t1-inset);
  color: var(--t1-muted);
}
.day-index {
  font-size: 18rpx;
  color: var(--t1-muted);
}
.plan-start {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  background: var(--t1-text);
  color: var(--t1-on-dark);
  font-size: 27rpx;
  font-weight: 700;
}
.guide-bottom {
  margin-top: 34rpx;
  font-size: 23rpx;
  color: var(--t1-muted);
  text-align: center;
}
.guide-bottom button {
  background: transparent;
  color: var(--t1-muted);
  font-size: 26rpx;
  padding: 20rpx 0;
  margin: 10rpx 0 0;
  line-height: 1.5;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 0 22rpx;
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  height: 88rpx;
}
.search-box > text {
  font-size: 40rpx;
  color: var(--t1-muted);
}
.search-box input {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  color: var(--t1-text);
}
.placeholder {
  color: var(--t1-muted);
}
.search-box button {
  margin: 0;
  padding: 0 12rpx;
  background: transparent;
  color: var(--t1-muted);
  font-size: 34rpx;
  line-height: 1.5;
}
.muscle-scroll {
  white-space: nowrap;
  width: 100%;
  margin: 22rpx 0 12rpx;
}
.filter-list {
  display: flex;
  gap: 12rpx;
  padding-bottom: 8rpx;
}
.filter-list button {
  margin: 0;
  flex-shrink: 0;
  min-width: 82rpx;
  padding: 14rpx 20rpx;
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 10rpx;
  color: var(--t1-muted);
  font-size: 24rpx;
  line-height: 1.5;
}
.filter-list button.selected {
  background: var(--t1-text);
  border-color: var(--t1-accent);
  color: var(--t1-on-dark);
  font-weight: 700;
}
.library-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 22rpx;
  color: var(--t1-muted);
  margin: 20rpx 0;
}
.equipment-filter {
  padding: 12rpx 0;
  color: var(--t1-muted);
}
.exercise-card {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 24rpx 20rpx;
  background: var(--t1-surface);
  border: 1px solid var(--t1-border);
  border-radius: 16rpx;
  margin-bottom: 14rpx;
}
.exercise-mark {
  width: 72rpx;
  height: 72rpx;
  line-height: 72rpx;
  flex-shrink: 0;
  text-align: center;
  border-radius: 14rpx;
  background: var(--t1-inset);
  color: var(--t1-accent);
  font-size: 30rpx;
  font-weight: 600;
}
.exercise-body {
  flex: 1;
  min-width: 0;
}
.exercise-name {
  font-size: 29rpx;
  font-weight: 600;
}
.exercise-meta {
  font-size: 20rpx;
  color: var(--t1-muted);
  margin: 6rpx 0;
}
.exercise-summary {
  font-size: 23rpx;
  line-height: 1.7;
  color: var(--t1-muted);
}
.exercise-arrow {
  color: var(--t1-muted);
  font-size: 28rpx;
}
.no-results {
  padding: 60rpx 10rpx;
  text-align: center;
  color: var(--t1-muted);
}
.no-results > text {
  font-size: 24rpx;
  color: var(--t1-muted);
  display: block;
  margin-top: 12rpx;
}
.no-results button {
  color: var(--t1-accent);
  background: var(--t1-tint);
  margin-top: 24rpx;
  font-size: 25rpx;
}
.detail-mask {
  position: fixed;
  z-index: 80;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.detail-sheet {
  max-width: 760px;
  width: 100%;
  background: var(--t1-surface);
  border-radius: 28rpx 28rpx 0 0;
  padding: 32rpx 32rpx calc(28rpx + env(safe-area-inset-bottom));
}
.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}
.detail-title {
  font-size: 40rpx;
  font-weight: 700;
  margin-top: 12rpx;
}
.detail-heading button {
  margin: 0;
  background: var(--t1-inset);
  color: var(--t1-muted);
  width: 64rpx;
  height: 64rpx;
  line-height: 58rpx;
  font-size: 40rpx;
  border-radius: 50%;
  padding: 0;
  flex-shrink: 0;
}
.detail-scroll {
  max-height: 55vh;
  margin: 24rpx 0;
}
.detail-summary {
  color: var(--t1-muted);
  font-size: 26rpx;
  margin-bottom: 28rpx;
}
.step {
  display: flex;
  gap: 22rpx;
  margin: 24rpx 0;
  line-height: 1.8;
  font-size: 27rpx;
}
.step > text {
  font-size: 20rpx;
  color: var(--t1-accent);
  padding-top: 6rpx;
  flex-shrink: 0;
}
.cues {
  background: var(--t1-inset);
  border: 1px solid var(--t1-border);
  border-radius: 14rpx;
  padding: 24rpx;
  margin-top: 28rpx;
  font-size: 24rpx;
  color: var(--t1-muted);
  line-height: 1.8;
}
.detail-cta {
  margin: 24rpx 0 0;
  background: var(--t1-text);
  color: var(--t1-on-dark);
  border-radius: 14rpx;
  font-size: 27rpx;
  font-weight: 700;
  min-height: 88rpx;
  line-height: 88rpx;
}
</style>
