export function patchText(el, text) {
  if (el && el.textContent !== text) el.textContent = text;
}

export function patchInner(el, html) {
  if (el && el.innerHTML !== html) el.innerHTML = html;
}

export function el(tag, className, html) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (html !== undefined) node.innerHTML = html;
  return node;
}
