<template>
  <main class="portal-editor">
    <header class="page-head">
      <h1>励志标语</h1>
      <div class="actions">
        <el-button :disabled="busy" @click="reload">重新载入</el-button>
        <el-button v-if="canEdit" type="primary" :loading="saving" :disabled="!dirty || busy" @click="save">保存修改</el-button>
      </div>
    </header>
    <el-alert v-if="feedback" class="feedback" :title="feedback" type="warning" :closable="false" show-icon />
    <div v-loading="loading" class="workspace">
      <template v-if="draft">
        <el-card shadow="never" class="editor-card">
          <el-form label-position="top" @submit.prevent="save">
            <section v-for="section in sections" :key="section.title" class="form-section">
              <h2>{{ section.title }}</h2>
              <div class="field-grid">
                <el-form-item v-for="field in section.fields" :key="field.key" :label="field.label" :for="'slogan-' + field.key"
                              :class="{ wide: field.wide }" :error="errors[field.key]">
                  <el-input :id="'slogan-' + field.key" v-model="draft[field.key]" :maxlength="field.limit" show-word-limit
                            :readonly="!canEdit || busy" @input="delete errors[field.key]" />
                </el-form-item>
              </div>
            </section>
          </el-form>
        </el-card>
        <aside class="preview" aria-label="首页标语预览">
          <h2>首页预览</h2>
          <div class="phone">
            <div class="photo-hero">
              <div class="brand"><img :src="brandImage" alt="" />T1 FITNESS</div>
              <div class="hero-copy">
                <div class="eyebrow">{{ draft.eyebrow }}</div>
                <div class="headline">
                  <div>{{ draft.titleLead }}<span>{{ draft.titleAccent }}</span></div>
                  <div>{{ draft.titleSecondLine }}</div>
                </div>
                <div class="subtitle">{{ draft.subtitle }}</div>
              </div>
            </div>
            <div class="phone-body">
              <div class="caption-row"><strong>{{ draft.galleryCaption }}</strong><span>看看空间 ↗</span></div>
              <div class="gallery"><img v-for="src in gallery" :key="src" :src="src" alt="" /></div>
              <div class="preview-button">开始训练 <span>→</span></div>
              <div class="preview-secondary">先看看训练计划 ↗</div>
              <div class="caption-row footer"><strong>{{ draft.footerCaption }}</strong><span>教练工作台 ↗</span></div>
            </div>
          </div>
        </aside>
      </template>
    </div>
  </main>
</template>

<script setup lang="ts" name="FitnessPortal">
import { ElMessage, ElMessageBox } from 'element-plus';
import { getPortalSlogans, updatePortalSlogans, type PortalSlogans } from '@/api/fitness/portal';
import { checkPermi } from '@/utils/permission';
import brandImage from '../../../../ui-item-file/图标.jpg';
import gymOne from '../../../../ui-item-file/实况图.jpg';
import gymTwo from '../../../../ui-item-file/实况图2.jpg';
import gymThree from '../../../../ui-item-file/实况图3.jpg';

type TextKey = Exclude<keyof PortalSlogans, 'version'>;
interface Field {
  key: TextKey;
  label: string;
  limit: number;
  wide?: boolean;
}
const sections: { title: string; fields: Field[] }[] = [
  { title: '首屏主标题', fields: [
    { key: 'eyebrow', label: '标题上方小字', limit: 32, wide: true },
    { key: 'titleLead', label: '第一行', limit: 12 },
    { key: 'titleAccent', label: '第一行橙色强调（选填）', limit: 8 },
    { key: 'titleSecondLine', label: '第二行', limit: 16, wide: true }
  ] },
  { title: '首屏副标题', fields: [{ key: 'subtitle', label: '主标题下方', limit: 60, wide: true }] },
  { title: '场馆照片区', fields: [{ key: 'galleryCaption', label: '照片上方左侧', limit: 40, wide: true }] },
  { title: '首页底部', fields: [{ key: 'footerCaption', label: '教练入口左侧', limit: 40, wide: true }] }
];
const fields = sections.flatMap((section) => section.fields);
const gallery = [gymOne, gymTwo, gymThree];
const canEdit = computed(() => checkPermi(['fitness:portal:edit']));
const draft = ref<PortalSlogans>();
const saved = ref<PortalSlogans>();
const errors = reactive<Partial<Record<TextKey, string>>>({});
const loading = ref(false);
const saving = ref(false);
const busy = computed(() => loading.value || saving.value);
const feedback = ref('');
const dirty = computed(() => !!draft.value && !!saved.value && fields.some(({ key }) => draft.value[key] !== saved.value[key]));

function adopt(record: PortalSlogans) {
  saved.value = { ...record };
  draft.value = { ...record };
  fields.forEach(({ key }) => delete errors[key]);
}

async function reload() {
  if (busy.value) return;
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('重新载入将替换当前未保存的文字。', '重新载入标语', {
        confirmButtonText: '重新载入', cancelButtonText: '保留编辑', type: 'warning'
      });
    } catch { return; }
  }
  loading.value = true;
  feedback.value = '';
  try {
    const { data } = await getPortalSlogans();
    adopt(data);
  } catch {
    feedback.value = '标语读取失败，请重新载入。';
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!canEdit.value || !dirty.value || busy.value) return;
  const changes = { ...draft.value };
  for (const field of fields) {
    const text = changes[field.key].trim();
    changes[field.key] = text;
    delete errors[field.key];
    if (!text && field.key !== 'titleAccent') errors[field.key] = '请填写' + field.label;
    else if (text.length > field.limit) errors[field.key] = '最多可填写 ' + field.limit + ' 个字符';
  }
  if (Object.keys(errors).length) return;
  saving.value = true;
  feedback.value = '';
  try {
    const { data } = await updatePortalSlogans(changes);
    adopt(data);
    ElMessage.success('标语已保存');
  } catch {
    feedback.value = '保存未成功，当前输入已保留。';
  } finally {
    saving.value = false;
  }
}

onMounted(reload);
</script>

<style scoped lang="scss">
.portal-editor { padding: 20px; max-width: 1180px; margin: 0 auto; }
.page-head, .actions, .caption-row, .brand, .preview-button { display: flex; align-items: center; }
.page-head, .caption-row, .preview-button { justify-content: space-between; gap: 14px; }
.page-head { margin-bottom: 18px; flex-wrap: wrap; }
h1, h2 { margin: 0; }
h1 { font-size: 24px; }
h2 { font-size: 16px; }
.feedback { margin-bottom: 14px; }
.workspace { display: grid; grid-template-columns: minmax(285px, 1fr) minmax(275px, 360px); gap: 24px; align-items: start; min-height: 140px; }
.form-section + .form-section { border-top: 1px solid var(--el-border-color-lighter); padding-top: 17px; }
.form-section h2 { margin-bottom: 14px; }
.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.wide { grid-column: 1 / -1; }
.preview { position: sticky; top: 12px; }
.preview h2 { margin-bottom: 12px; }
.phone { background: #101110; color: #f4f4f2; border-radius: 18px; overflow: hidden; overflow-wrap: anywhere; }
.photo-hero { min-height: 295px; padding: 20px 18px 28px; display: flex; flex-direction: column; justify-content: space-between; gap: 35px;
  background: linear-gradient(180deg, #10121c82, #1011109e 45%, #101110), url('../../../../ui-item-file/门户图片.jpg') center 30% / cover; }
.brand { justify-content: flex-start; gap: 8px; font-weight: 800; font-size: 13px; }
.brand img { width: 28px; height: 28px; border-radius: 5px; }
.eyebrow { display: flex; align-items: center; gap: 7px; color: #dedbd5; font-size: 10px; letter-spacing: 1.5px; font-weight: 700; }
.eyebrow::before { content: ''; width: 16px; height: 2px; background: #ff7a35; flex-shrink: 0; }
.headline { font-size: 37px; line-height: 1.12; margin: 12px 0; font-weight: 800; }
.headline span { color: #ff7a35; }
.subtitle { font-size: 13px; color: #d9d5cc; }
.phone-body { padding: 14px 18px; }
.caption-row { font-size: 11px; align-items: baseline; }
.caption-row span { flex-shrink: 0; color: #a8a79f; }
.gallery { display: grid; grid-template-columns: repeat(3, 1fr); gap: 5px; margin: 8px 0 16px; }
.gallery img { display: block; width: 100%; height: 57px; object-fit: cover; border-radius: 5px; }
.preview-button, .preview-secondary { padding: 12px; border-radius: 7px; font-weight: 700; font-size: 13px; }
.preview-button { background: #ff7a35; color: #16100e; }
.preview-secondary { margin-top: 6px; border: 1px solid #353733; text-align: center; color: #d5d3ce; }
.footer { border-top: 1px solid #33352f; color: #a8a79f; margin-top: 14px; padding-top: 12px; }
@media (max-width: 820px) { .workspace { grid-template-columns: 1fr; } .preview { position: static; max-width: 360px; width: 100%; } }
@media (max-width: 500px) { .portal-editor { padding: 12px; } .field-grid { grid-template-columns: 1fr; } }
</style>
