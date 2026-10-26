<template>
  <view class="t1-nav-wrap">
    <view class="t1-nav">
      <button
        v-for="item in items"
        :key="item.key"
        :class="['t1-nav-item', { selected: current === item.key }]"
        @tap="open(item)"
      >
        <view :class="['nav-symbol', item.icon]"
          ><view class="symbol-detail"
        /></view>
        <text>{{ item.label }}</text>
        <view v-if="current === item.key" class="nav-dot" />
      </button>
    </view>
  </view>
</template>
<script>
const memberItems = [
  { key: "home", label: "首页", icon: "home", url: "/pages/index/index" },
  {
    key: "course",
    label: "约课",
    icon: "calendar",
    url: "/pages/course/index",
  },
  {
    key: "training",
    label: "训练",
    icon: "barbell",
    url: "/pages/training/index",
  },
  { key: "mine", label: "我的", icon: "person", url: "/pages/mine/index" },
];
const coachItems = [
  {
    key: "home",
    label: "工作台",
    icon: "home",
    url: "/pages/coach/home/index",
  },
  {
    key: "schedule",
    label: "课表",
    icon: "calendar",
    url: "/pages/coach/schedule/index",
  },
  {
    key: "checkin",
    label: "签到",
    icon: "check",
    url: "/pages/coach/checkin/index",
  },
  {
    key: "student",
    label: "学员",
    icon: "people",
    url: "/pages/coach/student/index",
  },
  {
    key: "mine",
    label: "我的",
    icon: "person",
    url: "/pages/coach/mine/index",
  },
];
export default {
  props: {
    role: { type: String, default: "member" },
    current: { type: String, default: "home" },
  },
  computed: {
    items() {
      return this.role === "coach" ? coachItems : memberItems;
    },
  },
  methods: {
    open(item) {
      const pages = getCurrentPages();
      const page = pages[pages.length - 1];
      if (!page || `/${page.route}` !== item.url)
        uni.redirectTo({ url: item.url });
    },
  },
};
</script>
<style scoped>
.t1-nav-wrap {
  position: fixed;
  z-index: 20;
  left: 0;
  right: 0;
  bottom: 0;
  padding-bottom: env(safe-area-inset-bottom);
  background: #171816;
  border-top: 1px solid #30322f;
}
.t1-nav {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  padding: 12rpx 18rpx 8rpx;
}
.t1-nav-item {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 94rpx;
  padding: 8rpx 0 12rpx;
  margin: 0;
  background: none;
  border-radius: 0;
  color: #92968e;
  font-size: 21rpx;
  line-height: 1.4;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
}
.t1-nav-item::after {
  border: 0;
}
.t1-nav-item.selected {
  color: #ff7a35;
}
.nav-dot {
  width: 8rpx;
  height: 4rpx;
  border-radius: 2rpx;
  background: currentColor;
  position: absolute;
  bottom: 1rpx;
}
.nav-symbol {
  position: relative;
  width: 38rpx;
  height: 34rpx;
  color: inherit;
}
.home::before {
  content: "";
  position: absolute;
  width: 25rpx;
  height: 25rpx;
  left: 6rpx;
  top: 5rpx;
  border: 3rpx solid currentColor;
  border-radius: 4rpx;
}
.home .symbol-detail {
  position: absolute;
  left: 16rpx;
  bottom: 0;
  height: 13rpx;
  border-left: 3rpx solid currentColor;
}
.calendar {
  border: 3rpx solid currentColor;
  border-radius: 6rpx;
  width: 32rpx;
  height: 30rpx;
  margin-top: 2rpx;
}
.calendar::before {
  content: "";
  position: absolute;
  top: 6rpx;
  left: 0;
  right: 0;
  border-top: 3rpx solid currentColor;
}
.calendar .symbol-detail {
  position: absolute;
  left: 7rpx;
  top: 15rpx;
  width: 6rpx;
  height: 6rpx;
  background: currentColor;
  border-radius: 1rpx;
}
.barbell::before {
  content: "";
  position: absolute;
  left: 2rpx;
  right: 2rpx;
  top: 15rpx;
  height: 3rpx;
  background: currentColor;
}
.barbell::after {
  content: "";
  position: absolute;
  left: 7rpx;
  right: 7rpx;
  top: 5rpx;
  bottom: 5rpx;
  border-left: 5rpx solid currentColor;
  border-right: 5rpx solid currentColor;
}
.person::before,
.people::before {
  content: "";
  position: absolute;
  width: 12rpx;
  height: 12rpx;
  border: 3rpx solid currentColor;
  border-radius: 50%;
  top: 0;
  left: 12rpx;
}
.person::after,
.people::after {
  content: "";
  position: absolute;
  width: 28rpx;
  height: 14rpx;
  border: 3rpx solid currentColor;
  border-radius: 16rpx 16rpx 3rpx 3rpx;
  bottom: 0;
  left: 4rpx;
}
.people .symbol-detail {
  position: absolute;
  top: 7rpx;
  left: 0;
  height: 22rpx;
  width: 6rpx;
  border-left: 3rpx solid currentColor;
  border-radius: 10rpx;
}
.check {
  border: 3rpx solid currentColor;
  border-radius: 10rpx;
  width: 32rpx;
  height: 32rpx;
}
.check::after {
  content: "";
  position: absolute;
  left: 8rpx;
  top: 4rpx;
  width: 9rpx;
  height: 16rpx;
  border-bottom: 3rpx solid currentColor;
  border-right: 3rpx solid currentColor;
  transform: rotate(40deg);
}
</style>
