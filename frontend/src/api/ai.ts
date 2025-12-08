import request, { type ApiResponse } from '../utils/request'

export type AiChatRole = 'user' | 'assistant' | 'system'

export interface AiMessage {
  role: AiChatRole
  content: string
}

export interface AiChatResponse {
  answer: string
}

const unwrap = <T>(payload: ApiResponse<T> | T | undefined): T | undefined => {
  if (!payload) return undefined
  if (typeof payload === 'object' && 'data' in payload) {
    return (payload as ApiResponse<T>).data
  }
  return payload as T
}

export const sendAiChat = async (messages: AiMessage[]): Promise<string> => {
  const res = await request
    .post<ApiResponse<AiChatResponse> | AiChatResponse>('/api/ai/chat', { messages })
    .then((r) => r.data)

  const data = unwrap<AiChatResponse>(res)
  if (!data?.answer) {
    throw new Error('未收到 Mido AI 的回复')
  }
  return data.answer
}

