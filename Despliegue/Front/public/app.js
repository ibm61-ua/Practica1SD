// URL del API REST de tu EV_Central (Java), corriendo en el puerto 8083
const CENTRAL_API_URL = 'http://localhost:8082/api/status/all'; 
const statusContainer = document.getElementById('central-status'); // Contenedor HTML

// Función principal para obtener y mostrar datos
async function fetchCentralStatus() { 
    try {
        const response = await fetch(CENTRAL_API_URL); 
        
        if (!response.ok) {
            throw new Error(`Error en la respuesta del servidor: ${response.status}`);
        }
        
        const data = await response.json(); 
        
        renderStatus(data);
        
    } catch (error) {
    }
}

// Función para generar el HTML a partir de los datos JSON
function renderStatus(cps) {
    let htmlContent = '<h2>⚡ Estado de la Red de Recarga (Puntos de Recarga: ' + cps.length + ')</h2> <div class = "cp-cards">';
    
    cps.forEach(cp => {
        // Aseguramos que el estado esté en minúsculas y sin espacios para usarlo como clase CSS
        const statusClass = cp.status ? cp.status.toLowerCase().replace(/\s/g, '') : 'unknown';
        
        // El contenido del CP (revisa que los nombres de las propiedades coincidan con tu JSON de Java)
        htmlContent += `
            <div class="cp-card ${statusClass}">
                <h3>CP: ${cp.id}</h3>
                <p>📍 Ubicación: <b>${cp.location || 'N/A'}</b></p>
                <p>🟢 Estado: <b>${cp.status}</b></p>
                <p>€ Precio/kWh: <b>${cp.price}</b></p>
            </div>
        `;
    });

    htmlContent += '</div>'
    
    const statusContainer = document.getElementById('central-status'); 
    statusContainer.innerHTML = htmlContent;
}

// Inicia la consulta y la repite cada 3 segundos (refresco)
fetchCentralStatus(); 
setInterval(fetchCentralStatus, 3000);