import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../services/api';
import { MessageModal } from '../components/MessageModal';
import { usePageTitle } from '../context/PageTitleContext';

export function TransactionTypeForm() {
    const navigate = useNavigate();
    const { id } = useParams();
    const { setTitle } = usePageTitle();
    const [formData, setFormData] = useState({
        description: '',
        type: 'INCOME', // Default
        recurring: false,
        defaultDay: ''
    });
    const [loading, setLoading] = useState(false);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info', onClose: null });

    useEffect(() => {
        if (id) {
            setTitle('Alterar Tipo de Transação');
            fetchData();
        } else {
            setTitle('Novo Tipo de Transação');
        }
    }, [id]);

    const fetchData = async () => {
        try {
            const response = await api.get(`/transaction-types/${id}`);
            setFormData(response.data);
        } catch (error) {
            console.error('Erro ao carregar:', error);
            setMessageModal({
                isOpen: true,
                title: 'Erro',
                message: 'Erro ao carregar dados.',
                type: 'error',
                onClose: () => navigate('/transaction-types')
            });
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            if (id) {
                await api.put(`/transaction-types/${id}`, formData);
                setMessageModal({
                    isOpen: true,
                    title: 'Sucesso',
                    message: 'Tipo de transação atualizado com sucesso!',
                    type: 'success',
                    onClose: () => navigate('/transaction-types')
                });
            } else {
                await api.post('/transaction-types', formData);
                setMessageModal({
                    isOpen: true,
                    title: 'Sucesso',
                    message: 'Tipo de transação criado com sucesso!',
                    type: 'success',
                    onClose: () => navigate('/transaction-types')
                });
            }
        } catch (error) {
            console.error('Erro ao salvar:', error);
            const msg = error.response?.data?.message || 'Erro ao salvar.';
            setMessageModal({
                isOpen: true,
                title: 'Erro',
                message: msg,
                type: 'error'
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
            <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Descrição</label>
                        <input
                            type="text"
                            autoFocus
                            value={formData.description}
                            onChange={e => setFormData({ ...formData, description: e.target.value })}
                            required
                            maxLength={40}
                            placeholder="Ex: Salário, Aluguel..."
                            onInvalid={e => e.target.setCustomValidity('Por favor, informe a descrição.')}
                            onInput={e => e.target.setCustomValidity('')}
                        />
                    </div>

                    <div className="form-group">
                        <label>Tipo</label>
                        <select
                            value={formData.type}
                            onChange={e => setFormData({ ...formData, type: e.target.value })}
                            disabled={!!id} // Read-only on Edit
                            style={{ width: '100%', padding: '8px', marginTop: '5px', backgroundColor: id ? '#e9ecef' : '#fff' }}
                        >
                            <option value="INCOME">Entrada</option>
                            <option value="EXPENSE">Saída</option>
                        </select>
                        {id && <small style={{ color: '#666' }}>O tipo não pode ser alterado após a criação.</small>}
                    </div>

                    <div className="form-group">
                        <label>Recorrência</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <input
                                type="checkbox"
                                checked={formData.recurring}
                                onChange={e => setFormData({ ...formData, recurring: e.target.checked })}
                                id="recurring"
                                style={{ width: 'auto' }}
                            />
                            <label htmlFor="recurring" style={{ marginBottom: 0 }}>Sim, é recorrente</label>
                        </div>
                    </div>

                    <div className="form-group" style={{ marginTop: '15px' }}>
                        <label>Dia Padrão (Opcional)</label>
                        <input
                            type="number"
                            min="1"
                            max="31"
                            value={formData.defaultDay || ''}
                            onChange={e => {
                                const val = e.target.value;
                                if (val === '' || (parseInt(val) >= 1 && parseInt(val) <= 31)) {
                                    setFormData({ ...formData, defaultDay: val });
                                }
                            }}
                            placeholder="Dia (1-31)"
                            style={{ width: '100%', padding: '8px', marginTop: '5px' }}
                        />
                        <small style={{ color: '#666', display: 'block', marginTop: '5px' }}>Sugere este dia ao criar uma nova transação deste tipo.</small>
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                        <button type="submit" className="btn-primary" disabled={loading}>
                            {loading ? 'Salvando...' : 'Salvar'}
                        </button>
                        <button type="button" className="btn-secondary" onClick={() => navigate('/transaction-types')}>
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>

            <MessageModal
                isOpen={messageModal.isOpen}
                onClose={() => {
                    setMessageModal({ ...messageModal, isOpen: false });
                    if (messageModal.onClose) messageModal.onClose();
                }}
                title={messageModal.title}
                message={messageModal.message}
                type={messageModal.type}
            />
        </div>
    );
}
