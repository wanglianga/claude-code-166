// 轻量日期格式化：输出后端 LocalDateTime 需要的 'YYYY-MM-DDTHH:mm:ss'
export default function dayjsLite(date) {
  const p = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`
}
