const CENTRAL_API_URL = 'http://localhost:8081/api/status/all'; 

// Referencias al DOM
const cpContainer = document.getElementById('cp-container');
const logsContainer = document.getElementById('logs-container');
const driversList = document.getElementById('drivers-list');
const weatherContainer = document.getElementById('weather-container');
const alertContainer = document.getElementById('system-alerts');
const statusBadge = document.getElementById('connection-status');

// Función principal de Fetch
async function fetchCentralStatus() { 
    try {
        const response = await fetch(CENTRAL_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error server: ${response.status}`);
        }
        
        const data = await response.json(); 
        
        // Si llegamos aquí, hay conexión
        updateConnectionStatus(true);
        
        // Renderizamos todas las partes del Dashboard
        renderDashboard(data);
        
    } catch (error) {
        console.error("Fallo de conexión:", error);
        updateConnectionStatus(false);
    }
}

function updateConnectionStatus(isOnline) {
    if (isOnline) {
        statusBadge.textContent = "ONLINE";
        statusBadge.className = "badge online";
        statusBadge.style.backgroundColor = "#28a745";
    } else {
        statusBadge.textContent = "OFFLINE";
        statusBadge.className = "badge offline";
        statusBadge.style.backgroundColor = "#dc3545";
        alertContainer.innerHTML = '<div class="alert critical">¡ERROR CRÍTICO! No hay conexión con el Servidor Central (Java).</div>';
    }
}

// Función maestra de renderizado
function renderDashboard(data) {
    // 1. Renderizar Clima
    weatherContainer.innerHTML = `🌤️ ${data.weather || 'Sin datos'}`;

    // 2. Renderizar Alertas (Buscamos errores en los logs)
    const errors = data.logs.filter(log => log.includes("ERROR") || log.includes("Fallo"));
    if (errors.length > 0) {
        alertContainer.innerHTML = `
            <div class="alert warning">
                ⚠️ <strong>ALERTAS ACTIVAS:</strong> ${errors[errors.length - 1]}
            </div>`;
    } else {
        alertContainer.innerHTML = '';
    }

    // 3. Renderizar CPs
    let cpsHtml = '';
    if (data.cps.length === 0) cpsHtml = '<p>No hay CPs conectados.</p>';
    
    data.cps.forEach(cp => {
        // Lógica de colores y estado
        let statusClass = 'unknown';
        let icon = '⚪';
        
        if (cp.status === "CONECTADO") { statusClass = 'success'; icon = '🟢'; }
        if (cp.status === "CARGANDO")  { statusClass = 'charging'; icon = '⚡'; }
        if (cp.status === "AVERIADO")  { statusClass = 'danger'; icon = '🔴'; }
        if (cp.status === "PARADO")    { statusClass = 'warning'; icon = '⏸️'; }

        // Icono de autenticación
        const authIcon = cp.autenticado ? '🔒' : '🔓';
        
        // Info extra si está cargando
        let chargingInfo = '';
        if (cp.status === "CARGANDO") {
            chargingInfo = `
                <div class="charging-details">
                    <small>👤 Driver: ${cp.driver || '?'}</small><br>
                    <small>🔋 Energía: ${cp.KWHRequested} kWh</small>
                </div>
            `;
        }

        cpsHtml += `
            <div class="cp-card ${statusClass}">
                <div class="cp-header">
                    <h3>${cp.id}</h3>
                    <span>${icon}</span>
                </div>
                <div class="cp-body">
                    <p>📍 ${cp.location}</p>
                    <p>💰 ${cp.price} €/kWh</p>
                    <div class="cp-footer">
                        <span title="Autenticación">${authIcon} ${cp.autenticado ? 'Auth OK' : 'No Auth'}</span>
                        ${cp.temp > 0 ? `<span class="temp-badge">${cp.temp}°C</span>` : ''}
                    </div>
                    ${chargingInfo}
                </div>
            </div>
        `;
    });
    cpContainer.innerHTML = cpsHtml;

    // 4. Renderizar Drivers
    let driversHtml = '';
    if (data.drivers && data.drivers.length > 0) {
        data.drivers.forEach(d => {
            driversHtml += `<li>🚗 ${d}</li>`;
        });
    } else {
        driversHtml = '<li style="color:#777">Sin actividad de drivers.</li>';
    }
    driversList.innerHTML = driversHtml;

    // 5. Renderizar Logs
    let logsHtml = '';
    if (data.logs && data.logs.length > 0) {
        // Invertimos para ver el más nuevo arriba o lo dejamos normal
        data.logs.forEach(log => {
            // Pintamos de rojo si es error
            const colorStyle = log.includes("ERROR") ? 'style="color: #ff6b6b"' : '';
            logsHtml += `<div class="log-entry" ${colorStyle}><span class="arrow">></span> ${log}</div>`;
        });
    } else {
        logsHtml = '<div class="log-entry">Esperando logs...</div>';
    }
    logsContainer.innerHTML = logsHtml;
}

// Iniciar
fetchCentralStatus(); 
setInterval(fetchCentralStatus, 2000); // 2 segundos para mayor fluidez