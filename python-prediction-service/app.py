import os
import time
import random
import threading
import requests
from datetime import datetime
from flask import Flask, request, jsonify
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

FLASK_HOST = os.getenv('FLASK_HOST', '0.0.0.0')
FLASK_PORT = int(os.getenv('FLASK_PORT', 5002))
JAVA_SERVICE_URL = os.getenv('JAVA_INVENTORY_SERVICE_URL', 'http://localhost:8080')
LOW_LIFE_THRESHOLD = float(os.getenv('LOW_LIFE_THRESHOLD', 15))
CRITICAL_LIFE_THRESHOLD = float(os.getenv('CRITICAL_LIFE_THRESHOLD', 5))

machines = {
    'CNC-001': {'machine_id': 'CNC-001', 'name': 'CNC车削中心-01', 'status': 'running', 'status_updated_at': None, 'emergency_reason': None},
    'CNC-002': {'machine_id': 'CNC-002', 'name': 'CNC车削中心-02', 'status': 'running', 'status_updated_at': None, 'emergency_reason': None},
    'CNC-003': {'machine_id': 'CNC-003', 'name': 'CNC车削中心-03', 'status': 'running', 'status_updated_at': None, 'emergency_reason': None},
    'CNC-004': {'machine_id': 'CNC-004', 'name': 'CNC铣削中心-01', 'status': 'running', 'status_updated_at': None, 'emergency_reason': None}
}

cutting_tools = {
    'TOOL-001': {'tool_id': 'TOOL-001', 'model': 'CNMG-120408', 'machine_id': 'CNC-001', 'remaining_life': 100.0, 'total_usage_hours': 0.0, 'status': 'active', 'notified': False},
    'TOOL-002': {'tool_id': 'TOOL-002', 'model': 'VNMG-160408', 'machine_id': 'CNC-002', 'remaining_life': 100.0, 'total_usage_hours': 0.0, 'status': 'active', 'notified': False},
    'TOOL-003': {'tool_id': 'TOOL-003', 'model': 'WNMG-080408', 'machine_id': 'CNC-003', 'remaining_life': 100.0, 'total_usage_hours': 0.0, 'status': 'active', 'notified': False},
    'TOOL-004': {'tool_id': 'TOOL-004', 'model': 'CNMG-120408', 'machine_id': 'CNC-004', 'remaining_life': 100.0, 'total_usage_hours': 0.0, 'status': 'active', 'notified': False},
    'TOOL-005': {'tool_id': 'TOOL-005', 'model': 'DNMG-150408', 'machine_id': 'CNC-001', 'remaining_life': 100.0, 'total_usage_hours': 0.0, 'status': 'active', 'notified': False}
}

prediction_history = []


def read_machine_sensor_data(tool_id):
    base_current = {'CNC-001': 12.5, 'CNC-002': 15.2, 'CNC-003': 18.3, 'CNC-004': 14.0, 'CNC-001#2': 16.8}
    base_vibration = {'CNC-001': 0.35, 'CNC-002': 0.42, 'CNC-003': 0.55, 'CNC-004': 0.38, 'CNC-001#2': 0.48}
    
    tool = cutting_tools.get(tool_id)
    if not tool:
        return None
    
    machine_key = tool['machine_id']
    if tool_id == 'TOOL-005':
        machine_key = 'CNC-001#2'
    
    current = base_current.get(machine_key, 15.0) + random.uniform(-0.5, 0.5)
    vibration = base_vibration.get(machine_key, 0.4) + random.uniform(-0.05, 0.05)
    
    return {
        'tool_id': tool_id,
        'machine_id': tool['machine_id'],
        'timestamp': datetime.now().isoformat(),
        'current_amp': round(current, 2),
        'vibration_g': round(vibration, 3),
        'spindle_speed': random.randint(2000, 8000),
        'feed_rate': random.uniform(0.1, 0.5)
    }


def predict_remaining_life(sensor_data, tool_info):
    current = sensor_data['current_amp']
    vibration = sensor_data['vibration_g']
    usage_hours = tool_info['total_usage_hours']
    
    rated_current = 20.0
    rated_vibration = 1.0
    max_life_hours = 120.0
    
    current_ratio = current / rated_current
    vibration_ratio = vibration / rated_vibration
    usage_ratio = usage_hours / max_life_hours
    
    wear_factor = (0.4 * current_ratio + 0.3 * vibration_ratio + 0.3 * usage_ratio) * 100
    
    remaining_life = max(0.0, min(100.0, 100.0 - wear_factor))
    
    return round(remaining_life, 2)


def notify_inventory_service(tool_id, remaining_life, tool_model, machine_id):
    try:
        url = f"{JAVA_SERVICE_URL}/api/work-orders/life-alert"
        payload = {
            'toolId': tool_id,
            'toolModel': tool_model,
            'remainingLife': remaining_life,
            'threshold': LOW_LIFE_THRESHOLD,
            'machineId': machine_id,
            'timestamp': datetime.now().isoformat()
        }
        headers = {'Content-Type': 'application/json'}
        
        response = requests.post(url, json=payload, headers=headers, timeout=5)
        
        if response.status_code == 200:
            app.logger.info(f"通知库存服务成功: 刀具 {tool_id} 寿命 {remaining_life}%")
            return True, response.json()
        else:
            app.logger.error(f"通知库存服务失败: HTTP {response.status_code} - {response.text}")
            return False, None
    except Exception as e:
        app.logger.error(f"通知库存服务异常: {str(e)}")
        return False, None


def data_collection_loop():
    while True:
        for tool_id, tool_info in cutting_tools.items():
            if tool_info['status'] != 'active':
                continue
            
            machine = machines.get(tool_info['machine_id'])
            if machine and machine['status'] == 'emergency_stop':
                continue
            
            sensor_data = read_machine_sensor_data(tool_id)
            if not sensor_data:
                continue
            
            tool_info['total_usage_hours'] += 0.01
            remaining_life = predict_remaining_life(sensor_data, tool_info)
            tool_info['remaining_life'] = remaining_life
            
            history_record = {
                **sensor_data,
                'predicted_life': remaining_life,
                'usage_hours': round(tool_info['total_usage_hours'], 4)
            }
            prediction_history.append(history_record)
            if len(prediction_history) > 1000:
                prediction_history.pop(0)
            
            if remaining_life < LOW_LIFE_THRESHOLD and not tool_info['notified']:
                notified, resp = notify_inventory_service(tool_id, remaining_life, tool_info['model'], tool_info['machine_id'])
                if notified:
                    tool_info['notified'] = True
                    tool_info['status'] = 'pending_replacement'
                    
                    if resp and resp.get('data') and resp['data'].get('emergencyStopTriggered'):
                        app.logger.warning(f"生产线熔断已触发: 机床 {tool_info['machine_id']} 已紧急停机")
        
        time.sleep(2)


@app.route('/api/machines', methods=['GET'])
def get_all_machines():
    return jsonify({
        'success': True,
        'data': list(machines.values()),
        'timestamp': datetime.now().isoformat()
    })


@app.route('/api/machines/<machine_id>', methods=['GET'])
def get_machine(machine_id):
    machine = machines.get(machine_id)
    if not machine:
        return jsonify({'success': False, 'message': '机床不存在'}), 404
    return jsonify({'success': True, 'data': machine})


@app.route('/api/machines/<machine_id>/emergency-stop', methods=['POST'])
def trigger_emergency_stop(machine_id):
    machine = machines.get(machine_id)
    if not machine:
        return jsonify({'success': False, 'message': '机床不存在'}), 404
    
    data = request.get_json(silent=True) or {}
    reason = data.get('reason', '库存耗尽且刀具寿命危险，系统自动熔断')
    triggered_by = data.get('triggeredBy', 'system')
    
    machine['status'] = 'emergency_stop'
    machine['status_updated_at'] = datetime.now().isoformat()
    machine['emergency_reason'] = reason
    
    for tool_id, tool_info in cutting_tools.items():
        if tool_info['machine_id'] == machine_id:
            tool_info['status'] = 'machine_stopped'
    
    app.logger.critical(f"【紧急停机】机床 {machine_id} 已熔断停机，原因: {reason}，触发者: {triggered_by}")
    
    return jsonify({
        'success': True,
        'message': f'机床 {machine_id} 已触发紧急停机',
        'data': machine
    })


@app.route('/api/machines/<machine_id>/resume', methods=['POST'])
def resume_machine(machine_id):
    machine = machines.get(machine_id)
    if not machine:
        return jsonify({'success': False, 'message': '机床不存在'}), 404
    
    if machine['status'] != 'emergency_stop':
        return jsonify({'success': False, 'message': '机床未处于紧急停机状态'}), 400
    
    machine['status'] = 'running'
    machine['status_updated_at'] = datetime.now().isoformat()
    machine['emergency_reason'] = None
    
    for tool_id, tool_info in cutting_tools.items():
        if tool_info['machine_id'] == machine_id and tool_info['status'] == 'machine_stopped':
            tool_info['status'] = 'active'
    
    app.logger.info(f"机床 {machine_id} 已恢复运行")
    
    return jsonify({
        'success': True,
        'message': f'机床 {machine_id} 已恢复运行',
        'data': machine
    })


@app.route('/api/tools', methods=['GET'])
def get_all_tools():
    return jsonify({
        'success': True,
        'data': list(cutting_tools.values()),
        'timestamp': datetime.now().isoformat()
    })


@app.route('/api/tools/<tool_id>', methods=['GET'])
def get_tool(tool_id):
    tool = cutting_tools.get(tool_id)
    if not tool:
        return jsonify({'success': False, 'message': '刀具不存在'}), 404
    return jsonify({'success': True, 'data': tool})


@app.route('/api/tools/<tool_id>/predict', methods=['GET'])
def get_tool_prediction(tool_id):
    tool = cutting_tools.get(tool_id)
    if not tool:
        return jsonify({'success': False, 'message': '刀具不存在'}), 404
    
    sensor_data = read_machine_sensor_data(tool_id)
    remaining_life = predict_remaining_life(sensor_data, tool)
    
    return jsonify({
        'success': True,
        'data': {
            'tool_id': tool_id,
            'sensor_data': sensor_data,
            'predicted_remaining_life': remaining_life,
            'needs_replacement': remaining_life < LOW_LIFE_THRESHOLD,
            'critical_state': remaining_life < CRITICAL_LIFE_THRESHOLD,
            'threshold': LOW_LIFE_THRESHOLD,
            'critical_threshold': CRITICAL_LIFE_THRESHOLD
        }
    })


@app.route('/api/tools/<tool_id>/reset', methods=['POST'])
def reset_tool_life(tool_id):
    tool = cutting_tools.get(tool_id)
    if not tool:
        return jsonify({'success': False, 'message': '刀具不存在'}), 404
    
    tool['remaining_life'] = 100.0
    tool['total_usage_hours'] = 0.0
    tool['status'] = 'active'
    tool['notified'] = False
    
    return jsonify({
        'success': True,
        'message': f'刀具 {tool_id} 已重置',
        'data': tool
    })


@app.route('/api/history', methods=['GET'])
def get_prediction_history():
    limit = int(request.args.get('limit', 100))
    tool_id = request.args.get('tool_id')
    
    history = prediction_history
    if tool_id:
        history = [h for h in history if h['tool_id'] == tool_id]
    
    return jsonify({
        'success': True,
        'count': min(limit, len(history)),
        'data': history[-limit:]
    })


@app.route('/api/health', methods=['GET'])
def health_check():
    emergency_count = sum(1 for m in machines.values() if m['status'] == 'emergency_stop')
    return jsonify({
        'status': 'healthy',
        'service': 'prediction-service',
        'timestamp': datetime.now().isoformat(),
        'low_life_threshold': LOW_LIFE_THRESHOLD,
        'critical_life_threshold': CRITICAL_LIFE_THRESHOLD,
        'java_service_url': JAVA_SERVICE_URL,
        'emergency_stopped_machines': emergency_count
    })


@app.route('/api/manual-notify/<tool_id>', methods=['POST'])
def manual_notify(tool_id):
    tool = cutting_tools.get(tool_id)
    if not tool:
        return jsonify({'success': False, 'message': '刀具不存在'}), 404
    
    notified, resp = notify_inventory_service(tool_id, tool['remaining_life'], tool['model'], tool['machine_id'])
    
    return jsonify({
        'success': notified,
        'message': '手动通知已发送' if notified else '通知失败',
        'data': {
            'tool_id': tool_id,
            'remaining_life': tool['remaining_life'],
            'notified': notified,
            'response': resp
        }
    })


if __name__ == '__main__':
    data_thread = threading.Thread(target=data_collection_loop, daemon=True)
    data_thread.start()
    app.logger.info("数据采集线程已启动")
    app.logger.info(f"刀具寿命预警阈值: {LOW_LIFE_THRESHOLD}%")
    app.logger.info(f"刀具危险阈值: {CRITICAL_LIFE_THRESHOLD}%")
    app.logger.info(f"库存服务地址: {JAVA_SERVICE_URL}")
    app.run(host=FLASK_HOST, port=FLASK_PORT, debug=False)
