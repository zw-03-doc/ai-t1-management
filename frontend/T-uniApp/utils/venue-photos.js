let base = '/'
// #ifdef H5
base = import.meta.env.BASE_URL
// #endif

export const venuePhotos = ['live-1.jpg', 'live-2.jpg', 'live-3.jpg'].map(
  name => base + 'static/images/' + name
)
