const firebaseConfig = {
  apiKey: "AIzaSyD_nXUvuPfTeEmUYk6n_FhucVfuYhAntx0",
  databaseURL: "https://laprevia-restobar-default-rtdb.firebaseio.com",
  projectId: "laprevia-restobar",
  storageBucket: "laprevia-restobar.firebasestorage.app",
};

firebase.initializeApp(firebaseConfig);

const db = firebase.database();
const menuGrid = document.getElementById("menuGrid");
const statusEl = document.getElementById("status");
const filtersEl = document.getElementById("categoryFilters");
const tableBadge = document.getElementById("tableBadge");
const params = new URLSearchParams(window.location.search);
const tableNumber = params.get("mesa");

let allProducts = [];
let selectedCategory = "Todas";

tableBadge.textContent = tableNumber ? `Mesa ${tableNumber}` : "Carta";

db.ref("products").on(
  "value",
  (snapshot) => {
    const data = snapshot.val() || {};
    allProducts = Object.entries(data)
      .map(([id, product]) => ({ id, ...product }))
      .filter((product) => product.isActive !== false)
      .sort((a, b) => String(a.name || "").localeCompare(String(b.name || "")));

    renderFilters();
    renderMenu();
  },
  (error) => {
    statusEl.textContent = `No se pudo cargar la carta: ${error.message}`;
  }
);

function renderFilters() {
  const categories = ["Todas", ...new Set(allProducts.map((p) => p.category || "General"))].sort((a, b) => {
    if (a === "Todas") return -1;
    if (b === "Todas") return 1;
    return a.localeCompare(b);
  });

  filtersEl.innerHTML = "";
  categories.forEach((category) => {
    const button = document.createElement("button");
    button.className = `filter-button${selectedCategory === category ? " active" : ""}`;
    button.textContent = category;
    button.addEventListener("click", () => {
      selectedCategory = category;
      renderFilters();
      renderMenu();
    });
    filtersEl.appendChild(button);
  });
}

function renderMenu() {
  const visibleProducts = selectedCategory === "Todas"
    ? allProducts
    : allProducts.filter((product) => (product.category || "General") === selectedCategory);

  menuGrid.innerHTML = "";
  statusEl.textContent = visibleProducts.length
    ? `${visibleProducts.length} producto(s) disponible(s)`
    : "No hay productos activos en esta categoria.";

  visibleProducts.forEach((product) => {
    const card = document.createElement("article");
    card.className = "product-card";

    const image = product.imageUrl
      ? `<img class="product-image" src="${escapeHtml(product.imageUrl)}" alt="${escapeHtml(product.name || "Producto")}">`
      : "";

    card.innerHTML = `
      ${image}
      <div class="product-body">
        <h2 class="product-title">${escapeHtml(product.name || "Producto")}</h2>
        <p class="product-description">${escapeHtml(product.description || "Sin descripcion")}</p>
        <div class="product-footer">
          <span class="category">${escapeHtml(product.category || "General")}</span>
          <span class="price">S/ ${formatPrice(product.salePrice)}</span>
        </div>
      </div>
    `;
    menuGrid.appendChild(card);
  });
}

function formatPrice(value) {
  const number = Number(value || 0);
  return number.toFixed(2);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
