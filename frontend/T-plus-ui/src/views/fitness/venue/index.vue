<template>
  <main class="venue-manager" v-loading="busy">
    <header>
      <h1>场馆图片</h1>
      <div>
        <el-button :disabled="busy" @click="reload">刷新</el-button>
        <el-button v-if="canEdit" type="primary" :disabled="busy || !gallery" @click="choose()">上传图片</el-button>
      </div>
    </header>
    <input ref="picker" type="file" accept="image/jpeg,image/png" hidden @change="onFile" />
    <el-alert v-if="error" :title="error" type="warning" :closable="false" />
    <div v-if="gallery?.photos.length" class="photos">
      <article v-for="(photo, index) in gallery.photos" :key="photo.id">
        <el-image :src="imageSrc(photo)" fit="cover" :preview-src-list="gallery.photos.map(imageSrc)" :initial-index="index" preview-teleported />
        <footer v-if="canEdit">
          <div>
            <el-button link :disabled="busy || index === 0" @click="move(index, -1)">前移</el-button>
            <el-button link :disabled="busy || index === gallery.photos.length - 1" @click="move(index, 1)">后移</el-button>
          </div>
          <div>
            <el-button link :disabled="busy" @click="choose(photo.id)">更换</el-button>
            <el-button link type="danger" :disabled="busy" @click="remove(photo)">删除</el-button>
          </div>
        </footer>
      </article>
    </div>
    <el-empty v-else-if="gallery" description="暂无场馆照片" />
  </main>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { checkPermi } from '@/utils/permission';
import { deletePhoto, getGallery, reorderPhotos, uploadPhoto, type VenueGallery, type VenuePhoto } from '@/api/fitness/venue';

const canEdit = computed(() => checkPermi(['fitness:venue:edit']));
const gallery = ref<VenueGallery>();
const busy = ref(false);
const error = ref('');
const picker = ref<HTMLInputElement>();
let replaceId: string | undefined;

function imageSrc(photo: VenuePhoto) {
  return photo.url.startsWith('/app/static/images/') ? photo.url : import.meta.env.VITE_APP_BASE_API + photo.url;
}

async function reload() {
  if (busy.value) return;
  busy.value = true;
  try {
    gallery.value = (await getGallery()).data;
    error.value = '';
  } catch {
    error.value = '场馆图片读取失败，请刷新。';
  } finally { busy.value = false; }
}

async function save(action: (version: number) => Promise<{ data: VenueGallery }>) {
  if (!gallery.value || busy.value || !canEdit.value) return;
  busy.value = true;
  try {
    gallery.value = (await action(gallery.value.version)).data;
    error.value = '';
    ElMessage.success('已保存');
  } catch {
    error.value = '未确认保存结果，请刷新后重试。';
  } finally { busy.value = false; }
}

function choose(id?: string) {
  replaceId = id;
  picker.value?.click();
}

function onFile(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (file.size > 10 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.warning('请选择 10 MB 以内的 JPEG 或 PNG 图片');
    return;
  }
  const id = replaceId;
  void save(version => uploadPhoto(version, file, id));
}

function move(index: number, delta: number) {
  const ids = gallery.value?.photos.map(photo => photo.id);
  const to = index + delta;
  if (!ids || to < 0 || to >= ids.length) return;
  [ids[index], ids[to]] = [ids[to], ids[index]];
  void save(version => reorderPhotos(version, ids));
}

async function remove(photo: VenuePhoto) {
  try {
    await ElMessageBox.confirm('删除后，访客将无法查看这张照片。', '删除场馆照片', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    });
  } catch { return; }
  await save(version => deletePhoto(version, photo.id));
}
onMounted(reload);
</script>

<style scoped>
.venue-manager { max-width: 1160px; margin: 0 auto; padding: clamp(16px, 4vw, 40px); }
header, footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
header { flex-wrap: wrap; margin-bottom: 24px; }
h1 { font-size: 26px; margin: 0; }
.photos { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 245px), 1fr)); gap: 20px; }
article { overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 12px; background: var(--el-bg-color); }
.el-image { display: block; width: 100%; aspect-ratio: 4 / 3; cursor: zoom-in; }
footer { padding: 10px 12px; }
</style>
