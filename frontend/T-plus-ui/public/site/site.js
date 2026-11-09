const venueStore = new URLSearchParams(location.search).get('storeId')
function linkVenueStore(store) {
  for (const link of document.querySelectorAll('a[href^="/app/#/pages/portal/"]')) {
    const url = new URL(link.href)
    const [page, query] = url.hash.split('?')
    const params = new URLSearchParams(query)
    params.set('storeId', store)
    link.href = url.pathname + page + '?' + params.toString()
  }
}
async function showVenue() {
  if (venueStore !== null) {
    linkVenueStore(venueStore)
    if (!/^[1-9]\d{0,18}$/.test(venueStore)) return
  }
  const scope = venueStore ? '?storeId=' + venueStore : '';
  const response = await fetch('/prod-api/fitness/mini/portal/venue' + scope, { cache: 'no-store' });
  if (!response.ok) return;
  const result = await response.json();
  const path = result.data?.photos?.[0]?.url;
  if (result.code !== 200 || typeof path !== 'string' || (venueStore && String(result.data?.storeId) !== venueStore)) return;
  const bundled = /^\/app\/static\/images\/live-[123]\.jpg$/.test(path);
  const photoId = result.data.photos[0].id;
  if (!bundled && (!/^[0-9a-f-]{36}$/.test(photoId) || path !== '/fitness/mini/portal/venue/images/' + photoId + scope)) return;
  const image = document.querySelector('#venue-photo');
  image.onload = () => { image.parentElement.hidden = false; };
  image.src = bundled ? path : '/prod-api' + path;
}
// Preserve the workbench bookmark for administrators opening the domain root.
function openWorkbench() {
  try {
    if (venueStore === null && location.pathname === '/' && localStorage.getItem('Admin-Token')) {
      location.replace('/index');
      return true;
    }
  } catch { /* The public site also works with storage disabled. */ }
  return false;
}
if (!openWorkbench()) showVenue().catch(() => { /* All navigation stays available offline. */ });
