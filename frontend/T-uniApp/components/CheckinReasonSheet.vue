<template>
  <view class="reason-overlay" :style="{ bottom: keyboardInset + 'px' }">
    <view class="reason-backdrop" @tap="cancel" @touchmove.stop.prevent />
    <view class="reason-sheet" role="dialog" aria-modal="true" :aria-label="dialog.title">
      <view class="reason-body">
        <text class="reason-title">{{ dialog.title }}</text>
        <text class="reason-context">{{ dialog.item.memberName || "会员" }} · {{ dialog.item.courseName || "课程" }}</text>
        <text class="reason-label">{{ dialog.label }}</text>
        <textarea v-model="reason" class="reason-input" :aria-label="dialog.label"
          :placeholder="dialog.placeholder" :maxlength="500" :disabled="busy"
          :fixed="true" :adjust-position="false" :show-confirm-bar="false"
          @keyboardheightchange="keyboardChanged" />
        <text v-if="error" class="reason-error" role="alert">{{ error }}</text>
      </view>
      <view class="reason-actions">
        <button class="reason-cancel" :disabled="busy" @tap="cancel">取消</button>
        <button class="reason-submit" :disabled="busy || !reason.trim()" @tap="$emit('submit', reason.trim())">
          {{ busy ? "提交中…" : dialog.confirm }}
        </button>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    dialog: { type: Object, required: true },
    busy: Boolean,
    error: { type: String, default: "" },
  },
  emits: ["submit", "cancel"],
  data() {
    return { reason: "", keyboardInset: 0 };
  },
  mounted() {
    // #ifdef H5
    window.visualViewport?.addEventListener("resize", this.updateViewport);
    window.visualViewport?.addEventListener("scroll", this.updateViewport);
    window.addEventListener("keydown", this.onKeyDown);
    this.updateViewport();
    // #endif
  },
  beforeUnmount() {
    // #ifdef H5
    window.visualViewport?.removeEventListener("resize", this.updateViewport);
    window.visualViewport?.removeEventListener("scroll", this.updateViewport);
    window.removeEventListener("keydown", this.onKeyDown);
    // #endif
  },
  methods: {
    cancel() {
      if (!this.busy) this.$emit("cancel");
    },
    keyboardChanged(event) {
      // #ifdef MP-WEIXIN
      this.keyboardInset = Math.max(0, Number(event.detail.height) || 0);
      // #endif
    },
    // #ifdef H5
    updateViewport() {
      const viewport = window.visualViewport;
      this.keyboardInset = viewport ? Math.max(0, window.innerHeight - viewport.height - viewport.offsetTop) : 0;
    },
    onKeyDown(event) {
      if (event.key === "Escape") this.cancel();
    },
    // #endif
  },
};
</script>

<style scoped>
.reason-overlay { position: fixed; inset: 0; z-index: 100; display: flex; align-items: flex-end; justify-content: center; }
.reason-backdrop { position: absolute; inset: 0; background: rgba(0, 0, 0, .42); }
.reason-sheet {
  position: relative; width: 100%; max-width: 520px; max-height: 100%;
  display: flex; flex-direction: column; overflow: hidden;
  background: var(--t1-surface); color: var(--t1-text); border-radius: 18px 18px 0 0;
}
.reason-body { min-height: 0; padding: 22px 20px 4px; overflow-y: auto; }
.reason-title, .reason-context, .reason-label, .reason-error { display: block; }
.reason-title { font-size: 20px; font-weight: 650; line-height: 1.4; }
.reason-context { margin-top: 4px; line-height: 1.5; font-size: 14px; color: var(--t1-muted); overflow-wrap: anywhere; }
.reason-label { margin: 18px 0 8px; font-size: 15px; font-weight: 600; }
.reason-input {
  display: block; width: 100%; height: 132px; box-sizing: border-box; padding: 10px 12px;
  border: 1px solid var(--t1-border); border-radius: 8px; background: var(--t1-bg);
  font-size: 16px; line-height: 24px; color: var(--t1-text);
}
.reason-error { margin-top: 6px; color: var(--t1-danger); font-size: 14px; }
.reason-actions {
  flex-shrink: 0; display: flex; gap: 12px; padding: 12px 20px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
.reason-actions button { flex: 1; margin: 0; min-height: 46px; border-radius: 8px; font-size: 16px; font-weight: 600; line-height: 46px; }
.reason-cancel { color: var(--t1-text); background: var(--t1-inset); }
.reason-submit { color: var(--t1-on-dark); background: var(--t1-text); }
</style>
