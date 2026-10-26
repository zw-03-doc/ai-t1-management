<template>
  <view class="guide-page">
    <view class="guide-intro"
      ><view class="eyebrow">T1 / TRAINING NOTES</view
      ><view class="guide-title">练得有章法。</view
      ><view class="intro-copy">找到自己的节奏，再认真完成每一组。</view></view
    >
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
        动作指南 <text>24</text>
      </button></view
    >
    <view v-if="tab === 'plans'">
      <view v-for="plan in plans" :key="plan.key" class="plan-card">
        <view class="plan-cover"
          ><image :src="plan.image" mode="aspectFill" /><view
            class="cover-shade"
          /><view class="plan-cover-content"
            ><text class="plan-label">{{ plan.label }}</text
            ><view class="plan-title">{{ plan.name }}</view
            ><view class="plan-subtitle">{{ plan.subtitle }}</view></view
          ><text class="plan-number">{{ plan.number }}</text></view
        >
        <view class="plan-content"
          ><view class="plan-facts"
            ><text>{{ plan.cycle }}</text
            ><text>可自由调整</text><text>免费模板</text></view
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
      </view>
      <view class="note-card"
        ><view class="note-title">把计划变成你的节奏</view
        ><view class="note-row"
          ><text>01</text
          ><view
            ><view>安排训练与休息</view
            ><text>循环天数可以调整，不必固定在星期几。</text></view
          ></view
        ><view class="note-row"
          ><text>02</text
          ><view
            ><view>分别记录热身和正式组</view
            ><text>保留实际重量与次数，看清每次练了什么。</text></view
          ></view
        ><view class="note-row"
          ><text>03</text
          ><view
            ><view>临时有事，就顺延</view
            ><text>调整未完成的日程，已经完成的记录照常保留。</text></view
          ></view
        ></view
      >
    </view>
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
      ><text>按自己的训练经验调整动作和强度。</text
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
            ><view class="cues-title">留意这几点</view
            ><view v-for="cue in selected.cues" :key="cue"
              >· {{ cue }}</view
            ></view
          ></scroll-view
        ><button class="detail-cta" @tap="start()">
          去训练计划中添加 →
        </button></view
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
          number: "03",
          label: "PUSH / PULL / LEGS",
          name: "三分化训练",
          subtitle: "推 · 拉 · 腿",
          cycle: "4 天一循环",
          image: "/static/images/live-2.jpg",
          description: "把推、拉和腿部分开安排，三次训练后留一天休息。",
          days: ["推", "拉", "腿", "休息"],
        },
        {
          key: "five-split",
          number: "05",
          label: "ONE FOCUS AT A TIME",
          name: "五分化训练",
          subtitle: "胸 · 背 · 腿 · 肩 · 手臂",
          cycle: "7 天一循环",
          image: "/static/images/live-3.jpg",
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
  color: #f6f4ef;
}
.eyebrow {
  font-size: 19rpx;
  letter-spacing: 3rpx;
  color: #ff9c66;
}
.guide-title {
  font-size: 60rpx;
  font-weight: 800;
  line-height: 1.2;
  margin: 20rpx 0 16rpx;
}
.intro-copy {
  font-size: 25rpx;
  color: #a2a49c;
}
.tabs {
  display: flex;
  margin: 32rpx 0 26rpx;
  border-bottom: 1px solid #30322f;
  gap: 32rpx;
}
.tabs button {
  margin: 0;
  padding: 16rpx 0 20rpx;
  color: #a2a49c;
  background: transparent;
  font-size: 28rpx;
  line-height: 1.5;
  border-radius: 0;
}
.tabs button.active {
  color: #ff9c66;
  border-bottom: 3rpx solid #ff7a35;
  font-weight: 700;
}
.tabs button text {
  font-size: 19rpx;
  color: #858b7c;
  margin-left: 8rpx;
}
.plan-card {
  border: 1px solid #30322f;
  border-radius: 22rpx;
  overflow: hidden;
  margin-bottom: 26rpx;
  background: #1c1d1b;
}
.plan-cover {
  position: relative;
  min-height: 290rpx;
  overflow: hidden;
}
.plan-cover image,
.cover-shade {
  position: absolute;
  width: 100%;
  height: 100%;
  inset: 0;
}
.plan-cover image {
  opacity: 0.58;
}
.cover-shade {
  background: linear-gradient(
    90deg,
    rgba(16, 20, 13, 0.85),
    rgba(16, 20, 13, 0.12)
  );
}
.plan-cover-content {
  position: relative;
  padding: 30rpx;
  z-index: 1;
}
.plan-label {
  font-size: 17rpx;
  letter-spacing: 3rpx;
  color: #d6ddcc;
}
.plan-title {
  font-size: 45rpx;
  font-weight: 800;
  margin: 25rpx 0 10rpx;
}
.plan-subtitle {
  font-size: 25rpx;
  color: #d5dbce;
}
.plan-number {
  position: absolute;
  right: 25rpx;
  bottom: 0;
  font-size: 132rpx;
  font-weight: 800;
  line-height: 1;
  color: rgba(255, 255, 255, 0.18);
}
.plan-content {
  padding: 24rpx;
}
.plan-facts {
  display: flex;
  gap: 18rpx;
  color: #d1d5c9;
  font-size: 21rpx;
}
.plan-facts text + text {
  border-left: 1px solid #414637;
  padding-left: 18rpx;
}
.plan-description {
  font-size: 25rpx;
  color: #a2a49c;
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
  background: #2a2e24;
  border-radius: 10rpx;
  font-size: 21rpx;
  color: #e0e5d8;
}
.day.rest {
  background: #242620;
  color: #8d9582;
}
.day-index {
  font-size: 18rpx;
  color: #7e8871;
}
.plan-start {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 84rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  background: #ff7a35;
  color: #20160f;
  font-size: 27rpx;
  font-weight: 700;
}
.note-card {
  padding: 28rpx;
  background: #1c1d1b;
  border: 1px solid #30322f;
  border-radius: 20rpx;
}
.note-title {
  font-size: 31rpx;
  font-weight: 700;
  margin-bottom: 26rpx;
}
.note-row {
  display: flex;
  gap: 24rpx;
  margin: 22rpx 0;
}
.note-row > text {
  color: #ff9c66;
  font-size: 22rpx;
}
.note-row > view > view {
  font-size: 27rpx;
  margin-bottom: 6rpx;
}
.note-row > view > text {
  font-size: 23rpx;
  color: #a2a49c;
  line-height: 1.8;
}
.guide-bottom {
  margin-top: 34rpx;
  font-size: 23rpx;
  color: #8e9585;
  text-align: center;
}
.guide-bottom button {
  background: transparent;
  color: #dbe0d2;
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
  background: #1c1d1b;
  border: 1px solid #373c31;
  border-radius: 14rpx;
  height: 88rpx;
}
.search-box > text {
  font-size: 40rpx;
  color: #9da690;
}
.search-box input {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  color: #f6f4ef;
}
.placeholder {
  color: #7b8272;
}
.search-box button {
  margin: 0;
  padding: 0 12rpx;
  background: transparent;
  color: #a2a49c;
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
  background: #24271f;
  border: 1px solid #34392c;
  border-radius: 10rpx;
  color: #aeb6a1;
  font-size: 24rpx;
  line-height: 1.5;
}
.filter-list button.selected {
  background: #ff7a35;
  border-color: #ff7a35;
  color: #23180f;
  font-weight: 700;
}
.library-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 22rpx;
  color: #919a84;
  margin: 20rpx 0;
}
.equipment-filter {
  padding: 12rpx 0;
  color: #d0d8c3;
}
.exercise-card {
  display: flex;
  gap: 20rpx;
  align-items: center;
  padding: 24rpx 20rpx;
  background: #1c1d1b;
  border: 1px solid #30322f;
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
  background: #302a20;
  color: #f2b58e;
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
  color: #99a08d;
  margin: 6rpx 0;
}
.exercise-summary {
  font-size: 23rpx;
  line-height: 1.7;
  color: #b6bcae;
}
.exercise-arrow {
  color: #878f7b;
  font-size: 28rpx;
}
.no-results {
  padding: 60rpx 10rpx;
  text-align: center;
  color: #e0e4d9;
}
.no-results > text {
  font-size: 24rpx;
  color: #929b84;
  display: block;
  margin-top: 12rpx;
}
.no-results button {
  color: #ff9c66;
  background: #30271d;
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
  background: #1c1d1b;
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
  background: #30342a;
  color: #a2a49c;
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
  color: #b6bfaa;
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
  color: #ff9c66;
  padding-top: 6rpx;
  flex-shrink: 0;
}
.cues {
  background: #25291f;
  border: 1px solid #363c2e;
  border-radius: 14rpx;
  padding: 24rpx;
  margin-top: 28rpx;
  font-size: 24rpx;
  color: #b8c2aa;
  line-height: 1.8;
}
.cues-title {
  color: #e2e9d9;
  font-weight: 600;
  margin-bottom: 8rpx;
}
.detail-cta {
  margin: 24rpx 0 0;
  background: #ff7a35;
  color: #23180f;
  border-radius: 14rpx;
  font-size: 27rpx;
  font-weight: 700;
  min-height: 88rpx;
  line-height: 88rpx;
}
</style>
