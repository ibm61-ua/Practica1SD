// --- CONFIGURACIÓN DE APIs ---
const CENTRAL_API_URL = window.CONFIG.CENTRAL_API_URL;
const LOG_API_URL = window.CONFIG.LOG_API_URL;
const DRIVER_API_URL = window.CONFIG.DRIVER_API_URL; // Nueva constante

// --- REFERENCIAS AL DOM ---
const cpContainer = document.getElementById('cp-container');
const logsContainer = document.getElementById('logs-container');
const driversList = document.getElementById('drivers-list');
const weatherContainer = document.getElementById('weather-container');
const alertContainer = document.getElementById('system-alerts');
const statusBadge = document.getElementById('connection-status');

// =============================================================================
// 1. FETCH DE ESTADO DE CPs (Sistema Principal)
// =============================================================================
async function fetchSystemStatus() { 
    try {
        const response = await fetch(CENTRAL_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error CPs: ${response.status}`);
        }
        
        const data = await response.json(); 
        
        updateConnectionStatus(true);
        renderSystemData(data);
        
    } catch (error) {
        console.error("Fallo obteniendo CPs:", error);
        updateConnectionStatus(false);
    }
}

// =============================================================================
// 2. FETCH DE LOGS
// =============================================================================
async function fetchLogs() { 
    try {
        const response = await fetch(LOG_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error Logs: ${response.status}`);
        }
        
        const data = await response.json(); 
        renderLogsData(data);
        
    } catch (error) {
        console.error("Fallo obteniendo Logs:", error);
    }
}

// =============================================================================
// 3. FETCH DE DRIVERS (NUEVO)
// =============================================================================
async function fetchDrivers() { 
    try {
        const response = await fetch(DRIVER_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error Drivers: ${response.status}`);
        }
        
        // Se espera formato: ["FECHA HORA LUGAR VALOR ID", ...]
        const data = await response.json(); 
        renderDriversData(data);
        
    } catch (error) {
        console.error("Fallo obteniendo Drivers:", error);
    }
}

// =============================================================================
// GESTIÓN DE ESTADO DE CONEXIÓN
// =============================================================================
function updateConnectionStatus(isOnline) {
    if (!statusBadge) return;

    if (isOnline) {
        statusBadge.textContent = "ONLINE";
        statusBadge.className = "badge online";
        statusBadge.style.backgroundColor = "#28a745";
        statusBadge.style.color = "white";
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
// RENDERIZADO DE CPs (Sistema)
// =============================================================================
function renderSystemData(data) {
    const safeData = data || {};

    if (cpContainer) {
        let cps = [];
        // Soporte para array directo o objeto {cps: []}
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

                if (status === "CONECTADO") { statusClass = 'success'; icon = '🟢'; cardColor = '#62d07bff'; }
                else if (status === "CARGANDO")  { statusClass = 'charging'; icon = '⚡'; cardColor = '#22ff00ff'; }
                else if (status === "AVERIADO")  { statusClass = 'danger'; icon = '🔴'; cardColor = '#f53141ff'; }
                else if (status === "PARADO")    { statusClass = 'warning'; icon = '⏸️'; cardColor = '#ffb700ff'; }


                cpsHtml += `
                    <div class="cp-card ${statusClass}" style="background-color: ${cardColor}; border: 1px solid #ccc; border-radius: 8px; padding: 15px; width: 200px; box-shadow: 2px 2px 5px rgba(0,0,0,0.1);">
                        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                            <h3 style="margin:0;">${cp.id}</h3>
                            <span style="font-size:1.2em;">${icon}</span>
                        </div>
                        <div class="cp-body">
                            <p style="margin:5px 0;"> ${cp.location || 'N/A'}</p>
                            <p style="margin:5px 0;"> ${cp.price} €/kWh</p>
                            <div style="display:flex; justify-content:space-between; margin-top:10px; font-size:0.85em; color:#555;">
                                ${(cp.temp) ? `<span> ${cp.temp}°C</span>` : ''}
                            </div>
                        </div>
                    </div>
                `;
            });
        }
        cpContainer.innerHTML = cpsHtml;
    }
}

function renderDriversData(data) {
    if (!driversList) return;

    let drivers = [];
    if (Array.isArray(data)) {
        drivers = data;
    }

    let driversHtml = '';
    if (drivers.length > 0) {
        [...drivers].reverse().forEach(d => {
            driversHtml += `<li style="padding: 5px 0; border-bottom: 1px solid #eee; font-size: 0.9em;">
                 ${d}
            </li>`;
        });
    } else {
        driversHtml = '<li style="color:#999; font-style:italic;">Sin actividad de conductores.</li>';
    }
    driversList.innerHTML = driversHtml;
}

function renderLogsData(data) {
    let logs = [];
    if (Array.isArray(data)) {
        logs = data;
    } else if (data && Array.isArray(data.logs)) {
        logs = data.logs;
    }

    if (logsContainer) {
        let logsHtml = '';
        if (logs.length > 0) {
            [...logs].reverse().forEach(log => {
                const isError = log.includes("ERROR") || log.includes("Fallo") || log.includes("ALERTA");
                const colorStyle = isError ? 'color: #dc3545; font-weight:bold;' : 'color: #ffffffff;'; // Cambié blanco a oscuro para legibilidad si el fondo es claro
                logsHtml += `<div class="log-entry" style="${colorStyle} margin-bottom:4px; font-family:monospace;">
                                <span style="color:#aaa;">></span> ${log}
                             </div>`;
            });
        } else {
            logsHtml = '<div style="color:#999; font-style:italic;">Esperando registros del sistema...</div>';
        }
        logsContainer.innerHTML = logsHtml;
    }

    if (alertContainer) {
        const errors = logs.filter(log => log && (log.includes("ERROR") || log.includes("ALERTA")));
        
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


fetchSystemStatus(); 
fetchLogs();
fetchDrivers();

setInterval(fetchSystemStatus, 2000);
setInterval(fetchLogs, 4000);
setInterval(fetchDrivers, 2000);