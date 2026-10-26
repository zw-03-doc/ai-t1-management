<template>
  <view class="page"
    ><view class="title">学员关注的课程</view
    ><view class="hint"
      >仅展示与你关联的学员。时长来自用户同意后的前台浏览，供沟通参考。</view
    ><view v-if="error" @tap="load">{{ error }} · 点击重试</view
    ><view
      v-for="item in list"
      :key="item.memberId + '-' + item.courseId"
      class="card"
      ><view class="name">{{ item.memberName }} · {{ item.courseName }}</view
      ><view class="row"
        ><text
          >有效浏览
          {{ Math.round((item.activeSeconds / 60) * 10) / 10 }} 分钟</text
        ><text>{{ item.viewCount }} 次访问</text></view
      ><view class="hint">最近浏览 {{ item.lastViewedAt }}</view
      ><button size="mini" @tap="training(item)">为该学员定制计划</button></view
    ><view v-if="!list.length && !error" class="card"
      >暂无课程兴趣记录。</view
    ></view
  >
</template>
<script>
import { request } from "../../../utils/request";
export default {
  data() {
    return { list: [], error: "" };
  },
  onShow() {
    this.load();
  },
  methods: {
    async load() {
      try {
        const result = await request({
          url: "/fitness/coachApp/interest/list",
          auth: "coach",
          data: { pageNum: 1, pageSize: 100 },
        });
        this.list = Array.isArray(result) ? result : result?.rows || [];
        this.error = "";
      } catch (e) {
        this.error = e.message;
      }
    },
    training(item) {
      uni.navigateTo({
        url: `/pages/training/index?role=coach&memberId=${item.memberId}`,
      });
    },
  },
};
</script>
<style scoped>
.page {
  max-width: 850px;
  margin: auto;
  padding: 30rpx;
}
.title {
  font-size: 40rpx;
  font-weight: 700;
}
.hint {
  font-size: 25rpx;
  color: #6f7f77;
  line-height: 1.8;
  margin: 16rpx 0;
}
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-top: 24rpx;
}
.name {
  font-size: 30rpx;
  font-weight: 600;
}
.row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  margin: 24rpx 0;
}
button {
  color: #246348;
}
</style>
