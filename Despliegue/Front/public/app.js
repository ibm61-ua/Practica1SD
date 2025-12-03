const CENTRAL_API_URL = 'http://localhost:3000/api/status/all'; 

// Referencias al DOM
const cpContainer = document.getElementById('cp-container');
const logsContainer = document.getElementById('logs-container');
const driversList = document.getElementById('drivers-list');
const weatherContainer = document.getElementById('weather-container');
const alertContainer = document.getElementById('system-alerts');
const statusBadge = document.getElementById('connection-status');

// --- FUNCIÓN PRINCIPAL DE FETCH ---
async function fetchCentralStatus() { 
    try {
        const response = await fetch(CENTRAL_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error server: ${response.status}`);
        }
        
        const data = await response.json(); 
        
        // Debug en consola para ver qué llega realmente
        // console.log("Datos recibidos:", data);

        // Si llegamos aquí, hay conexión
        updateConnectionStatus(true);
        
        // Renderizamos el Dashboard con los datos
        renderDashboard(data);
        
    } catch (error) {
        console.error("Fallo de conexión:", error);
        updateConnectionStatus(false);
    }
}

// --- ACTUALIZAR INDICADOR ONLINE/OFFLINE ---
function updateConnectionStatus(isOnline) {
    if (!statusBadge) return; // Protección si no existe el elemento HTML

    if (isOnline) {
        statusBadge.textContent = "ONLINE";
        statusBadge.className = "badge online";
        statusBadge.style.backgroundColor = "#28a745";
        statusBadge.style.color = "white";
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

// --- FUNCIÓN MAESTRA DE RENDERIZADO ---
function renderDashboard(data) {
    
    // 1. PREPARACIÓN DE DATOS (PROTECCIÓN CONTRA NULOS)
    // Esto evita el error "cannot read properties of undefined"
    
    // Si data es null, usamos objeto vacío
    const safeData = data || {}; 

    // Detectamos si 'data' es directamente el array de CPs o un objeto complejo
    let cps = [];
    if (Array.isArray(safeData)) {
        cps = safeData;
    } else if (Array.isArray(safeData.cps)) {
        cps = safeData.cps;
    }

    // Listas seguras (si no existen en el JSON, usamos arrays vacíos)
    const logs = Array.isArray(safeData.logs) ? safeData.logs : [];
    const drivers = Array.isArray(safeData.drivers) ? safeData.drivers : [];
    const weather = safeData.weather || 'Sin datos meteorológicos';

    // 2. RENDERIZAR CLIMA
    if (weatherContainer) {
        weatherContainer.innerHTML = `🌤️ ${weather}`;
    }

    // 3. RENDERIZAR ALERTAS (Basado en los logs seguros)
    if (alertContainer) {
        const errors = logs.filter(log => log && (log.includes("ERROR") || log.includes("Fallo")));
        
        if (errors.length > 0) {
            // Mostramos el último error
            const lastError = errors[errors.length - 1];
            alertContainer.innerHTML = `
                <div class="alert warning" style="background:#fff3cd; color:#856404; padding:10px; margin-bottom:10px; border: 1px solid #ffeeba; border-radius:5px;">
                    ⚠️ <strong>ALERTA ACTIVA:</strong> ${lastError}
                </div>`;
        } else {
            alertContainer.innerHTML = ''; // Limpiar si no hay errores
        }
    }

    // 4. RENDERIZAR CPs
    if (cpContainer) {
        let cpsHtml = '';
        if (cps.length === 0) {
            cpsHtml = '<p style="text-align:center; color:#666;">No hay puntos de carga conectados.</p>';
        } else {
            cps.forEach(cp => {
                // Normalizamos estado a mayúsculas para evitar errores de texto
                const status = (cp.status || 'DESCONOCIDO').toUpperCase();
                
                let statusClass = 'unknown'; // Clase CSS por defecto
                let icon = '⚪';
                let cardColor = '#f8f9fa'; // Color de fondo por defecto

                // Asignar estilos según estado
                if (status === "CONECTADO") { statusClass = 'success'; icon = '🟢'; cardColor = '#d4edda'; }
                else if (status === "CARGANDO")  { statusClass = 'charging'; icon = '⚡'; cardColor = '#fff3cd'; }
                else if (status === "AVERIADO")  { statusClass = 'danger'; icon = '🔴'; cardColor = '#f8d7da'; }
                else if (status === "PARADO")    { statusClass = 'warning'; icon = '⏸️'; cardColor = '#e2e3e5'; }

                
                // Detalles extra si está cargando
                let chargingInfo = '';
                if (status === "CARGANDO") {
                    chargingInfo = `
                        <div style="margin-top:10px; padding-top:10px; border-top:1px solid #ccc; font-size:0.9em;">
                            <strong>Cargando:</strong><br>
                             ${cp.driver || 'Desconocido'}<br>
                             ${cp.KWHRequested || 0} kWh solicitados
                        </div>
                    `;
                }

                // Construcción de la tarjeta
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

    // 5. RENDERIZAR DRIVERS
    if (driversList) {
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

    // 6. RENDERIZAR LOGS
    if (logsContainer) {
        let logsHtml = '';
        if (logs.length > 0) {
            // Clonamos y revertimos para ver lo más nuevo arriba
            [...logs].reverse().forEach(log => {
                const isError = log.includes("ERROR") || log.includes("Fallo");
                const colorStyle = isError ? 'color: #dc3545; font-weight:bold;' : 'color: #333;';
                logsHtml += `<div class="log-entry" style="${colorStyle} margin-bottom:4px; font-family:monospace;">
                                <span style="color:#aaa;">></span> ${log}
                             </div>`;
            });
        } else {
            logsHtml = '<div style="color:#999; font-style:italic;">Esperando registros del sistema...</div>';
        }
        logsContainer.innerHTML = logsHtml;
    }
}

// Iniciar
fetchCentralStatus(); 
setInterval(fetchCentralStatus, 3000);