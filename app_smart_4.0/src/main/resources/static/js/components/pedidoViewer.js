import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';

// ─── Dimensões — espelhadas de 3d-em-react/blockModel.ts ────────────────────
const BW = 1.7, BD = 1.7, BH = 0.82;
const BASE_T        = 0.14;
const COL_W         = 0.19, COL_OVERSHOOT = 0.2;
const BLADE_T       = 0.08, BLADE_RECESS  = 0.14; // recess positivo = recuo para dentro
const LID_H         = 0.22;

// ─── Paleta de cores ─────────────────────────────────────────────────────────
const COR_BLOCO = { PRETO: 0x252527, VERMELHO: 0xCC2222, AZUL: 0x1A55CC, VAZIO: 0x252527 };
const COR_LAMINA = {
  VERMELHO: 0xE6463F, AZUL: 0x1A55CC, AMARELO: 0xE6B800,
  VERDE: 0x229944, PRETO: 0x484848, BRANCO: 0xF0F0EE,
};
const COR_TAMPA = { PRETO: 0x252527, VERMELHO: 0xCC2222, AZUL: 0x1A55CC };

// ─── Mapeamentos de @JsonValue dos enums Java ────────────────────────────────
// CorBloco / CorTampa: VAZIO=0, PRETO=1, VERMELHO=2, AZUL=3
const INT_COR_BLOCO  = { 0: 'VAZIO', 1: 'PRETO', 2: 'VERMELHO', 3: 'AZUL' };
// CorLamina: VERMELHO=1, AZUL=2, AMARELO=3, VERDE=4, PRETO=5, BRANCO=6
const INT_COR_LAMINA = { 1: 'VERMELHO', 2: 'AZUL', 3: 'AMARELO', 4: 'VERDE', 5: 'PRETO', 6: 'BRANCO' };
// PosicaoLamina: ESQUERDA=1, FRENTE=2, DIREITA=3
const INT_POSICAO    = { 1: 'ESQUERDA', 2: 'FRENTE', 3: 'DIREITA' };

const AUTO_ROTATE_SPEED  = 2.0;
const RESUME_DELAY_MS    = 2500;
// Cada bloco superior é ligeiramente maior que o inferior para que as faces externas
// nunca sejam coplanares com as colunas do bloco de baixo (elimina z-fighting definitivamente).
const DELTA_POR_NIVEL    = 0.005;

// ─── Helpers de geometria ────────────────────────────────────────────────────

function mat(hex, opts = {}) {
  return new THREE.MeshStandardMaterial({ color: hex, roughness: 0.55, metalness: 0, ...opts });
}

// Cria um mesh de caixa colorido sem posição definida.
// Posicione com mesh.position.set(x, y, z) e adicione ao group desejado.
// opts é repassado para o material — use para polygonOffset, depthWrite, etc.
export function box(w, h, d, hex, opts = {}) {
  return new THREE.Mesh(new THREE.BoxGeometry(w, h, d), mat(hex, opts));
}

// ─── Normalização ────────────────────────────────────────────────────────────

function normBlocoCor(v) {
  return typeof v === 'number' ? (INT_COR_BLOCO[v] ?? 'PRETO') : (v ?? 'PRETO');
}

function normLaminaCor(v) {
  return typeof v === 'number' ? (INT_COR_LAMINA[v] ?? 'VERMELHO') : (v ?? 'VERMELHO');
}

export function normPosicao(v) {
  return typeof v === 'number' ? (INT_POSICAO[v] ?? 'FRENTE') : (v ?? 'FRENTE');
}

// ─── Tampa ───────────────────────────────────────────────────────────────────

function criarTampa(corVal, totalH) {
  const nome = normBlocoCor(corVal);
  const hex  = COR_TAMPA[nome] ?? 0x252527;
  const m    = box(BW + 0.01, LID_H, BD + 0.01, hex);
  m.position.set(0, totalH + LID_H / 2, 0);
  return m;
}

// ─── Bloco ───────────────────────────────────────────────────────────────────
// Construção da geometria de um bloco individual.
// Eixos: X = largura, Y = altura, Z = profundidade (câmera em +Z).
// Faces abertas: FRENTE=+Z, ESQUERDA=-X, DIREITA=+X.
// Face traseira (-Z) é sempre fechada com uma parede sólida.

function criarBloco(corVal, laminas, yOffset, delta = 0) {
  const group = new THREE.Group();

  const bw = BW + delta;
  const bd = BD + delta;
  const nome        = normBlocoCor(corVal);
  const hex         = COR_BLOCO[nome] ?? 0x252527;
  const bodyH       = BH - BASE_T;
  const bodyCenterY = yOffset + BASE_T + bodyH / 2;

  // ① Piso base
  const piso = box(bw, BASE_T, bd, hex);
  piso.position.set(0, yOffset + BASE_T / 2, 0);
  group.add(piso);

  // ② 4 colunas dos cantos
  const colOpts = { polygonOffset: true, polygonOffsetFactor: 4, polygonOffsetUnits: 8 };
  const cx = bw / 2 - COL_W / 2;
  const cz = bd / 2 - COL_W / 2;
  [[-cx, -cz], [cx, -cz], [-cx, cz], [cx, cz]].forEach(([x, z]) => {
    const col = box(COL_W, bodyH + COL_OVERSHOOT, COL_W, hex, colOpts);
    col.position.set(x, bodyCenterY + COL_OVERSHOOT / 2, z);
    group.add(col);
  });

  // ③ Parede traseira (-Z)
  const parede = box(bw - 2 * COL_W, bodyH, 0.08, hex);
  parede.position.set(0, bodyCenterY, -bd / 2 + COL_W - 0.04);
  group.add(parede);

  // ④ Lâminas coloridas nas faces abertas (FRENTE=+Z, ESQUERDA=-X, DIREITA=+X)
  const abertura = bw - 2 * COL_W;
  const xBlade   = bw / 2 - BLADE_RECESS - BLADE_T / 2;
  const zBlade   = bd / 2 - BLADE_RECESS - BLADE_T / 2;

  laminas.forEach((lamina) => {
    const lHex = COR_LAMINA[normLaminaCor(lamina.cor)] ?? 0xE6463F;
    const face = normPosicao(lamina.posicaoNoBloco);
    let m;
    if (face === 'FRENTE') {
      m = box(abertura, bodyH, BLADE_T, lHex);
      m.position.set(0, bodyCenterY, zBlade);
    } else if (face === 'ESQUERDA') {
      m = box(BLADE_T, bodyH, abertura, lHex);
      m.position.set(-xBlade, bodyCenterY, 0);
    } else {
      m = box(BLADE_T, bodyH, abertura, lHex);
      m.position.set(xBlade, bodyCenterY, 0);
    }
    group.add(m);
  });

  return group;
}

// ─── Gestão da cena ──────────────────────────────────────────────────────────

function limparGrupo(g) {
  while (g.children.length) {
    const c = g.children[0];
    g.remove(c);
    descartarObj(c);
  }
}

function descartarObj(obj) {
  obj.geometry?.dispose();
  const mats = obj.material
    ? (Array.isArray(obj.material) ? obj.material : [obj.material])
    : [];
  mats.forEach((m) => m.dispose());
  obj.children?.forEach(descartarObj);
}

function adicionarLuzes(scene) {
  scene.add(new THREE.AmbientLight(0xffffff, 0.75));
  const d1 = new THREE.DirectionalLight(0xffffff, 0.7);
  d1.position.set(4, 6, 5);
  scene.add(d1);
  const d2 = new THREE.DirectionalLight(0xffffff, 0.3);
  d2.position.set(-4, 2, -4);
  scene.add(d2);
}

// Percorre todos os descendentes e define renderOrder, garantindo que blocos
// superiores (maior índice) sejam desenhados depois. Com o depth test LEQUAL do
// WebGL, o fragmento desenhado por último vence em caso de empate de profundidade.
function definirOrdem(obj, ordem) {
  obj.traverse((node) => { node.renderOrder = ordem; });
}

function construirCena(root, pedido) {
  limparGrupo(root);
  if (!pedido) return;

  const blocos = pedido.blocos ?? [];
  if (!blocos.length) return;

  const totalH = BH * blocos.length;
  root.position.y = -totalH / 2;

  blocos.forEach((bloco, i) => {
    const grupo = criarBloco(bloco.cor, bloco.laminas ?? [], i * BH, i * DELTA_POR_NIVEL);
    definirOrdem(grupo, i);
    root.add(grupo);
  });

  const tampa = criarTampa(pedido.corTampa, totalH);
  definirOrdem(tampa, blocos.length);
  root.add(tampa);
}

// ─── API pública ─────────────────────────────────────────────────────────────

/**
 * Cria um viewer 3D do pedido dentro de `container`.
 * O container deve ter largura e altura definidas via CSS.
 *
 * Uso:
 *   const viewer = createPedidoViewer(document.querySelector('.pedido-viewer'));
 *   viewer.update(pedidoData);   // passa dados do pedido (response DTO)
 *   viewer.dispose();            // limpa ao desmontar
 */
export function createPedidoViewer(container) {
  container.classList.add('pedido-viewer--vazio');

  const renderer = new THREE.WebGLRenderer({ antialias: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setClearColor(0xe8e1f2, 1);
  container.appendChild(renderer.domElement);

  const camera = new THREE.PerspectiveCamera(38, 1, 0.1, 100);
  camera.position.set(0, 1.0, 8);

  const scene = new THREE.Scene();
  adicionarLuzes(scene);

  const root = new THREE.Group();
  scene.add(root);

  const controls = new OrbitControls(camera, renderer.domElement);
  controls.enablePan      = false;
  controls.minDistance    = 3;
  controls.maxDistance    = 16;
  controls.autoRotate     = true;
  controls.autoRotateSpeed = AUTO_ROTATE_SPEED;

  let timer = null;
  controls.addEventListener('start', () => { clearTimeout(timer); controls.autoRotate = false; });
  controls.addEventListener('end',   () => {
    timer = setTimeout(() => { controls.autoRotate = true; }, RESUME_DELAY_MS);
  });

  const ro = new ResizeObserver(([entry]) => {
    const { width: w, height: h } = entry.contentRect;
    if (!w || !h) return;
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
    renderer.setSize(w, h, false);
  });
  ro.observe(container);

  const { width: w0, height: h0 } = container.getBoundingClientRect();
  renderer.setSize(w0 || 300, h0 || 300, false);
  camera.aspect = (w0 || 1) / (h0 || 1);
  camera.updateProjectionMatrix();

  let raf;
  const loop = () => { raf = requestAnimationFrame(loop); controls.update(); renderer.render(scene, camera); };
  loop();

  return {
    update(pedido) {
      container.classList.toggle('pedido-viewer--vazio', !pedido);
      construirCena(root, pedido);
    },
    dispose() {
      cancelAnimationFrame(raf);
      clearTimeout(timer);
      ro.disconnect();
      controls.dispose();
      limparGrupo(root);
      renderer.dispose();
      renderer.domElement.remove();
    },
  };
}
