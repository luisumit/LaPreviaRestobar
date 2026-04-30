const express = require('express');
const admin = require('firebase-admin');

const app = express();
app.use(express.json());

// credenciales Firebase
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: "https://laprevia-restobar-default-rtdb.firebaseio.com"
});

const db = admin.database();


// 🔵 obtener usuarios
app.get('/usuarios', async (req, res) => {
  try {
    const snapshot = await db.ref('users').once('value');
    const data = snapshot.val();
    if (!data) return res.status(200).json([]);
    return res.status(200).json(Object.values(data));
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// 🟢 guardar usuario
app.post('/usuarios', async (req, res) => {
  try {
    const data = req.body;
    await db.ref('users').push(data);
    return res.json({ mensaje: "Usuario guardado" });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});


// 🔵 obtener inventario
app.get('/inventario', async (req, res) => {
  try {
    const snapshot = await db.ref('inventory').once('value');
    const data = snapshot.val();
    if (!data) return res.status(200).json([]);
    return res.status(200).json(Object.values(data));
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// 🟢 guardar inventario
app.post('/inventario', async (req, res) => {
  try {
    const data = req.body;
    await db.ref('inventory').push(data);
    return res.json({ mensaje: "Producto agregado al inventario" });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});


// 🔵 obtener pedidos
app.get('/orders', async (req, res) => {
  try {
    const snapshot = await db.ref('orders').once('value');
    const data = snapshot.val();
    if (!data) return res.status(200).json([]);
    return res.status(200).json(Object.values(data));
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// 🟢 guardar pedido
app.post('/orders', async (req, res) => {
  try {
    const data = req.body;
    await db.ref('orders').push(data);
    return res.json({ mensaje: "Pedido registrado" });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});


// 🔵 obtener productos
app.get('/products', async (req, res) => {
  try {
    const snapshot = await db.ref('products').once('value');
    const data = snapshot.val();
    if (!data) return res.status(200).json([]);
    return res.status(200).json(Object.values(data));
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// 🟢 guardar producto
app.post('/products', async (req, res) => {
  try {
    const data = req.body;
    await db.ref('products').push(data);
    return res.json({ mensaje: "Producto guardado" });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});


// 🔵 obtener mesas
app.get('/tables', async (req, res) => {
  try {
    const snapshot = await db.ref('tables').once('value');
    const data = snapshot.val();
    if (!data) return res.status(200).json([]);
    return res.status(200).json(Object.values(data));
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// 🟢 guardar mesa
app.post('/tables', async (req, res) => {
  try {
    const data = req.body;
    await db.ref('tables').push(data);
    return res.json({ mensaje: "Mesa registrada" });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});


// 🚀 iniciar servidor
app.listen(3000, () => {
  console.log("API Firebase corriendo en puerto 3000");
});
