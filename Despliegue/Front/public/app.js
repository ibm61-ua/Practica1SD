// --- CONSTANTES API ---
const CENTRAL_API_URL = 'http://localhost:3000/api/status/cp';  // CPs, Drivers, Weather
const LOG_API_URL = 'http://localhost:3000/api/status/log';     // Solo Logs

// --- REFERENCIAS AL DOM ---
const cpContainer = document.getElementById('cp-container');
const logsContainer = document.getElementById('logs-container');
const driversList = document.getElementById('drivers-list');
const weatherContainer = document.getElementById('weather-container');
const alertContainer = document.getElementById('system-alerts');
const statusBadge = document.getElementById('connection-status');

// =============================================================================
// 1. FETCH DE ESTADO DEL SISTEMA (CPs, Drivers, Clima)
//    Se ejecuta frecuentemente (tiempo real)
// =============================================================================
async function fetchSystemStatus() { 
    try {
        const response = await fetch(CENTRAL_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error CPs: ${response.status}`);
        }
        
        const data = await response.json(); 
        
        // Si obtenemos los CPs, consideramos que el sistema está ONLINE
        updateConnectionStatus(true);
        
        renderSystemData(data);
        
    } catch (error) {
        console.error("Fallo obteniendo CPs:", error);
        updateConnectionStatus(false);
    }
}

// =============================================================================
// 2. FETCH DE LOGS
//    Se puede ejecutar con menos frecuencia para no saturar
// =============================================================================
async function fetchLogs() { 
    try {
        const response = await fetch(LOG_API_URL); 
        
        if (!response.ok) {
            // No marcamos offline todo el sistema si solo fallan los logs, 
            // pero lo mostramos en consola
            throw new Error(`Error Logs: ${response.status}`);
        }
        
        const data = await response.json(); 
        renderLogsData(data);
        
    } catch (error) {
        console.error("Fallo obteniendo Logs:", error);
    }
}

// --- ACTUALIZAR INDICADOR ONLINE/OFFLINE ---
function updateConnectionStatus(isOnline) {
    if (!statusBadge) return;

    if (isOnline) {
        statusBadge.textContent = "ONLINE";
        statusBadge.className = "badge online";
        statusBadge.style.backgroundColor = "#28a745";
        statusBadge.style.color = "white";
        // Limpiamos alerta crítica de conexión si vuelve
        if (alertContainer && alertContainer.innerHTML.includes("Sin conexión")) {
            alertContainer.innerHTML = '';
        }
    } else {
        statusBadge.textContent = "OFFLINE";
        statusBadge.className = "badge offline";
        statusBadge.style.backgroundColor = "#dc3545";
        statusBadge.style.color = "white";
        
        if(alertContainer) {
            alertContainer.innerHTML = '<div class="alert critical" style="background:#f8d7da; color:#721c24; padding:10px; margin-bottom:10px; border: 1px solid #f5c6cb; border-radius:5px;">⚠️ <strong>ERROR CRÍTICO:</strong> Sin conexión con la Central (Puerto 3000).</div>';
        }
    }
}

// =============================================================================
// RENDERIZADO PARTE A: CPs, DRIVERS, CLIMA
// =============================================================================
function renderSystemData(data) {
    const safeData = data || {};

    // 2. RENDERIZAR CPs
    if (cpContainer) {
        // Detectamos si data es un array o un objeto con propiedad cps
        let cps = [];
        if (Array.isArray(safeData)) {
            cps = safeData;
        } else if (Array.isArray(safeData.cps)) {
            cps = safeData.cps;
        }

        let cpsHtml = '';
        if (cps.length === 0) {
            cpsHtml = '<p style="text-align:center; color:#666;">No hay puntos de carga conectados.</p>';
        } else {
            cps.forEach(cp => {
                const status = (cp.status || 'DESCONOCIDO').toUpperCase();
                let statusClass = 'unknown';
                let icon = '⚪';
                let cardColor = '#f8f9fa';

                if (status === "CONECTADO") { statusClass = 'success'; icon = '🟢'; cardColor = '#d4edda'; }
                else if (status === "CARGANDO")  { statusClass = 'charging'; icon = '⚡'; cardColor = '#fff3cd'; }
                else if (status === "AVERIADO")  { statusClass = 'danger'; icon = '🔴'; cardColor = '#f8d7da'; }
                else if (status === "PARADO")    { statusClass = 'warning'; icon = '⏸️'; cardColor = '#e2e3e5'; }

                
                let chargingInfo = '';
                if (status === "CARGANDO") {
                    chargingInfo = `
                        <div style="margin-top:10px; padding-top:10px; border-top:1px solid #ccc; font-size:0.9em;">
                            <strong>Cargando:</strong><br>
                            👤 ${cp.driver || '?'}<br>
                            🔋 ${cp.KWHRequested || 0} kWh
                        </div>
                    `;
                }

                cpsHtml += `
                    <div class="cp-card ${statusClass}" style="background-color: ${cardColor}; border: 1px solid #ccc; border-radius: 8px; padding: 15px; width: 200px; box-shadow: 2px 2px 5px rgba(0,0,0,0.1);">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                            <h3 style="margin:0;">${cp.id}</h3>
                            <span style="font-size:1.2em;">${icon}</span>
                        </div>
                        <div class="cp-body">
                            <p style="margin:5px 0;">📍 ${cp.location || 'N/A'}</p>
                            <p style="margin:5px 0;">💰 ${cp.price} €/kWh</p>
                            <div style="display:flex; justify-content:space-between; margin-top:10px; font-size:0.85em; color:#555;">
                                ${(cp.temp && cp.temp > 0) ? `<span>🌡️ ${cp.temp}°C</span>` : ''}
                            </div>
                            ${chargingInfo}
                        </div>
                    </div>
                `;
            });
        }
        cpContainer.innerHTML = cpsHtml;
    }

    // 3. RENDERIZAR DRIVERS
    if (driversList) {
        const drivers = Array.isArray(safeData.drivers) ? safeData.drivers : [];
        let driversHtml = '';
        if (drivers.length > 0) {
            drivers.forEach(d => {
                driversHtml += `<li style="padding: 5px 0; border-bottom: 1px solid #eee;">🚗 ${d}</li>`;
            });
        } else {
            driversHtml = '<li style="color:#999; font-style:italic;">Sin actividad reciente.</li>';
        }
        driversList.innerHTML = driversHtml;
    }
}

// =============================================================================
// RENDERIZADO PARTE B: LOGS Y ALERTAS DE SISTEMA
// =============================================================================
function renderLogsData(data) {
    // Aceptamos que data pueda ser un array directo ["log1", "log2"] o un objeto { logs: [] }
    let logs = [];
    if (Array.isArray(data)) {
        logs = data;
    } else if (data && Array.isArray(data.logs)) {
        logs = data.logs;
    }

    // 1. RENDERIZAR LOGS
    if (logsContainer) {
        let logsHtml = '';
        if (logs.length > 0) {
            [...logs].reverse().forEach(log => {
                const isError = log.includes("ERROR") || log.includes("Fallo") || log.includes("ALERTA");
                const colorStyle = isError ? 'color: #dc3545; font-weight:bold;' : 'color: #ffffffff;';
                logsHtml += `<div class="log-entry" style="${colorStyle} margin-bottom:4px; font-family:monospace;">
                                <span style="color:#aaa;">></span> ${log}
                             </div>`;
            });
        } else {
            logsHtml = '<div style="color:#999; font-style:italic;">Esperando registros del sistema...</div>';
        }
        logsContainer.innerHTML = logsHtml;
    }

    // 2. RENDERIZAR ALERTAS (Basado en los logs recibidos)
    if (alertContainer) {
        // Filtramos mensajes críticos
        const errors = logs.filter(log => log && (log.includes("ERROR") || log.includes("ALERTA")));
        
        // Solo mostramos alerta si hay errores Y no hay un error crítico de conexión ya mostrándose
        if (errors.length > 0 && !alertContainer.innerHTML.includes("Sin conexión")) {
            const lastError = errors[errors.length - 1];
            alertContainer.innerHTML = `
                <div class="alert warning" style="background:#fff3cd; color:#856404; padding:10px; margin-bottom:10px; border: 1px solid #ffeeba; border-radius:5px;">
                    ⚠️ <strong>ALERTA DE SISTEMA:</strong> ${lastError}
                </div>`;
        } else if (errors.length === 0 && !alertContainer.innerHTML.includes("Sin conexión")) {
            alertContainer.innerHTML = ''; 
        }
    }
}

// --- INICIO DE EJECUCIÓN ---

// Llamada inicial
fetchSystemStatus(); 
fetchLogs();

// 1. Refresco rápido para CPs (Tiempo real - 2 seg)
setInterval(fetchSystemStatus, 2000); 

// 2. Refresco medio para Logs (Para no saturar - 4 seg)
setInterval(fetchLogs, 4000);