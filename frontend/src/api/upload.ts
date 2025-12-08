import request from '../utils/request'

export interface UploadImageResult {
  url?: string
  path?: string
  message?: string
}

export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return request
    .post<UploadImageResult>('/api/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((res) => res.data)
}

export default {
  uploadImage,
}

