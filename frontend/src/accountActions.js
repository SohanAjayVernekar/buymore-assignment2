import { api, store } from './api'

export async function buyAgain(id) {
  const result = await api(`/account/orders/${id}/buy-again`)
  let added = 0, limited = 0
  for (const { product, quantity } of result.items) {
    const item = store.cart.find(i => i.product_id === product._id)
    const count = Math.min(quantity, 100 - (item?.quantity || 0))
    if (count < quantity) limited++
    if (!count) continue
    if (item) Object.assign(item, { title: product.title, unit_price: product.price, category: product.category, quantity: item.quantity + count })
    else if (store.cart.length < 100) store.cart.push({ product_id: product._id, title: product.title, unit_price: product.price, category: product.category, quantity: count })
    else { limited++; continue }
    added += count
  }
  if (added) store.cartBump++
  return `${added ? `${added} item${added === 1 ? '' : 's'} added at current prices. Review your cart before ordering.` : 'No items were added.'}${result.unavailable.length ? ` Unavailable: ${result.unavailable.join(', ')}.` : ''}${limited ? ' Some items reached the cart limit.' : ''}`
}
