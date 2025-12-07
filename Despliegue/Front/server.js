const express = require('express'); 
const path = require('path'); 

const app = express();

const port = process.env.PORT || 8080;
const host = process.env.HOST || 'localhost';

// --- NUEVO: Variables para el Front-end ---
const apiCp = process.env.API_CP
const apiLog = process.env.API_LOG
const apiDriver = process.env.API_DRIVER

app.get('/config.js', (req, res) => {
    res.set('Content-Type', 'application/javascript');
    res.send(`
        window.CONFIG = {
            CENTRAL_API_URL: "${apiCp}",
            LOG_API_URL: "${apiLog}",
            DRIVER_API_URL: "${apiDriver}"
        };
    `);
});

app.use(express.static(path.join(__dirname, 'public')));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(port, () => {
  console.log(`Servidor iniciado en http://${host}:${port}`);
});