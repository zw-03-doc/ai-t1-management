<template>
  <view class="consent-mask" @touchmove.stop.prevent>
    <view class="consent-sheet" role="dialog" aria-modal="true" aria-label="登录前请阅读">
      <text class="consent-title">登录前请阅读</text>
      <view class="consent-copy">
        请阅读<navigator url="/pages/legal/index?doc=terms" class="consent-link">《用户服务协议》</navigator>和<navigator url="/pages/legal/index?doc=privacy" class="consent-link">《隐私政策》</navigator>，了解账号、预约和训练记录的使用方式。
      </view>
      <button class="consent-agree" @tap="$emit('agree')">同意并继续登录</button>
      <button class="consent-decline" @tap="$emit('decline')">不同意，继续浏览</button>
    </view>
  </view>
</template>

<script>
export default { emits: ['agree', 'decline'] }
</script>

<style scoped>
.consent-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(25, 25, 22, .40);
}
.consent-sheet {
  box-sizing: border-box;
  width: 100%;
  max-width: 370px;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  color: #242522;
  text-align: left;
}
.consent-title { display: block; font-size: 21px; font-weight: 650; }
.consent-copy { margin: 14px 0 22px; font-size: 15px; line-height: 1.75; color: #51534d; }
.consent-link { display: inline; color: #22231f; text-decoration: underline; }
.consent-agree, .consent-decline { width: 100%; margin: 0; border-radius: 9px; line-height: 45px; font-size: 15px; }
.consent-agree { background: #252621; color: #fff; }
.consent-decline { margin-top: 7px; background: #f4f4f1; color: #373931; }
</style>
