export const parseDate = (time) => {
  if (time === null || time === undefined) return null
  
  // 处理时间戳 (数字或字符串形式的数字)
  if (typeof time === 'number' || (typeof time === 'string' && /^\d+$/.test(time))) {
    return new Date(Number(time))
  }

  // 处理数组 [year, month, day, hour, minute, second]
  if (Array.isArray(time)) {
     // 确保至少有年月日
     if (time.length < 3) return null
     return new Date(time[0], time[1] - 1, time[2], time[3] || 0, time[4] || 0, time[5] || 0)
  }
  
  // 处理字符串
  if (typeof time === 'string') {
    // 兼容 iOS/Safari: "yyyy-MM-dd HH:mm:ss" -> "yyyy-MM-ddTHH:mm:ss"
    return new Date(time.indexOf('T') === -1 ? time.replace(' ', 'T') : time)
  }
  
  // 处理对象 (Java 8 time 序列化可能出现的 {year: 2024, ...})
  if (typeof time === 'object') {
     if (time.year && time.monthValue && time.dayOfMonth) {
       return new Date(time.year, time.monthValue - 1, time.dayOfMonth, time.hour || 0, time.minute || 0, time.second || 0)
     }
  }
  
  return new Date(time)
}

export const formatTime = (time) => {
  try {
    const date = parseDate(time)
    if (!date || isNaN(date.getTime())) {
      console.warn('Invalid date:', time)
      return ''
    }
    return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0')
  } catch (e) {
    console.error('Format time error:', e)
    return ''
  }
}

export const formatTimeForList = (time) => {
  try {
    const date = parseDate(time)
    if (!date || isNaN(date.getTime())) return ''
    
    const now = new Date()
    if (date.toDateString() === now.toDateString()) {
      return date.getHours().toString().padStart(2, '0') + ':' + date.getMinutes().toString().padStart(2, '0')
    } else {
      return (date.getMonth() + 1).toString().padStart(2, '0') + '-' + date.getDate().toString().padStart(2, '0')
    }
  } catch (e) {
    return ''
  }
}
