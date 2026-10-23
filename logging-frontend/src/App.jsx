import { useEffect, useState } from 'react';
import './App.css';

const API_ORDERS = 'http://localhost:8080/api/orders';
const API_LOGS = 'http://localhost:8080/api/logs'; // endpoint de la actividad anterior de logs

function App() {
  // Estados para la Actividad de DLX / DLQ
  const [backend, setBackend] = useState('CHECKING');
  const [metrics, setMetrics] = useState({ ordersReady: 0, dlqReady: 0, alert: false });
  const [lastOrderResponse, setLastOrderResponse] = useState(null);

  // Estados para la Actividad Anterior de Logs
  const [logMessage, setLogMessage] = useState('');
  const [logType, setLogType] = useState('INFO');
  const [lastLogResponse, setLastLogResponse] = useState(null);

  // --- MÉTODOS ACTIVIDAD DLX / DLQ ---
  async function refreshMetrics() {
    try {
      const [statusRes, metricsRes] = await Promise.all([
        fetch(`${API_ORDERS}/status`),
        fetch(`${API_ORDERS}/metrics`)
      ]);
      const statusData = await statusRes.json();
      const metricsData = await metricsRes.json();

      setBackend(statusData.rabbitmq === 'CONNECTED' ? 'CONNECTED' : 'DISCONNECTED');
      setMetrics(metricsData);
    } catch (error) {
      setBackend('DISCONNECTED');
    }
  }

  useEffect(() => {
    refreshMetrics();
    const id = setInterval(refreshMetrics, 2000);
    return () => clearInterval(id);
  }, []);

  async function sendOrder(forceFail) {
    const body = {
      orderId: `ORD-${Date.now()}`,
      customerName: forceFail ? 'Cliente DLQ' : 'Cliente OK',
      forceFail: String(forceFail)
    };

    const response = await fetch(`${API_ORDERS}/send`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });

    const data = await response.json();
    setLastOrderResponse(data);

    setTimeout(refreshMetrics, 800);
    setTimeout(refreshMetrics, 3500);
  }

  // --- metodos de la actividad de logs ---
  async function handleSendLog(e) {
    e.preventDefault();
    try {
      const response = await fetch(API_LOGS, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: logMessage,
          type: logType,
          timestamp: new Date().toISOString()
        })
      });
      const data = await response.json();
      setLastLogResponse(data);
      setLogMessage('');
    } catch (err) {
      setLastLogResponse({ error: 'No se pudo conectar con el endpoint de logs' });
    }
  }

  return (
    <main className="container">
      <h1>Panel - Logging & RabbitMQ DLQ</h1>

      {/* ================= modulo de la nueva actividad dlx y dlq ================= */}
      <section className="card">
        <h2>Pruebas DLX / DLQ </h2>
        <p>Estado Backend/RabbitMQ: <b>{backend}</b></p>
        <p>Mensajes listos en cola principal: <b>{metrics.ordersReady}</b></p>
        <p>Mensajes en DLQ: <b>{metrics.dlqReady}</b></p>

        {metrics.alert && (
          <div className="alert">
            ALERTA: existen mensajes en orders.dlq. Revise RabbitMQ Management y los logs del backend.
          </div>
        )}

        <div className="actions" style={{ marginTop: '16px' }}>
          <button onClick={() => sendOrder(false)}>Enviar orden exitosa</button>
          <button onClick={() => sendOrder(true)}>Enviar orden que debe ir a DLQ</button>
          <button onClick={refreshMetrics}>Actualizar métricas</button>
        </div>

        {lastOrderResponse && (
          <div style={{ marginTop: '16px' }}>
            <h4>Última respuesta HTTP (Órdenes):</h4>
            <pre>{JSON.stringify(lastOrderResponse, null, 2)}</pre>
          </div>
        )}
      </section>

      {/* ================= modulo de la actividad anterior de logs ================= */}
      <section className="card">
        <h2>Módulo de Logs</h2>
        <form onSubmit={handleSendLog} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '4px' }}>Nivel de Log:</label>
            <select value={logType} onChange={(e) => setLogType(e.target.value)} style={{ padding: '8px', width: '100%' }}>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '4px' }}>Mensaje:</label>
            <input
              type="text"
              value={logMessage}
              onChange={(e) => setLogMessage(e.target.value)}
              placeholder="Escribe un mensaje de prueba..."
              required
              style={{ padding: '8px', width: '100%' }}
            />
          </div>
          <button type="submit" style={{ width: 'fit-content' }}>Enviar Log</button>
        </form>

        {lastLogResponse && (
          <div style={{ marginTop: '16px' }}>
            <h4>Respuesta de envío de log:</h4>
            <pre>{JSON.stringify(lastLogResponse, null, 2)}</pre>
          </div>
        )}
      </section>
    </main>
  );
}

export default App;