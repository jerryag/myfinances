import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { MessageModal } from '../components/MessageModal';
import { FaEdit, FaTrash, FaSearch } from 'react-icons/fa';
import { usePageTitle } from '../context/PageTitleContext';

export function TransactionTypeList() {
    const navigate = useNavigate();
    const { setTitle } = usePageTitle();
    const [transactionTypes, setTransactionTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filters, setFilters] = useState({ description: '', types: [] });
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    // Message Modal state
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info' });

    // Confirmation Modal state
    const [modalOpen, setModalOpen] = useState(false);
    const [pendingAction, setPendingAction] = useState(null);

    useEffect(() => {
        setTitle('Tipos de Transação');
        fetchTransactionTypes();
    }, [page, filters]); // Re-fetch when page or filters change

    const fetchTransactionTypes = async () => {
        setLoading(true);
        try {
            const params = {
                page,
                size: 10,
                sort: 'description,asc',
                description: filters.description,
                types: filters.types.join(',')
            };

            // Remove empty params
            if (!params.description) delete params.description;
            if (filters.types.length === 0) delete params.types;

            const response = await api.get('/transaction-types', { params });
            setTransactionTypes(response.data.content);
            setTotalPages(response.data.totalPages);
        } catch (error) {
            console.error('Erro ao buscar tipos de transação:', error);
            setMessageModal({
                isOpen: true,
                title: 'Erro',
                message: 'Erro ao carregar dados.',
                type: 'error'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (e) => {
        const { name, value, checked, type } = e.target;
        if (type === 'checkbox') {
            setFilters(prev => {
                const newTypes = checked
                    ? [...prev.types, value]
                    : prev.types.filter(t => t !== value);
                return { ...prev, types: newTypes };
            });
        } else {
            setFilters(prev => ({ ...prev, [name]: value }));
        }
        setPage(0); // Reset to first page on filter change
    };

    const handleDeleteClick = (id) => {
        setPendingAction(() => async () => {
            try {
                await api.delete(`/transaction-types/${id}`);
                fetchTransactionTypes();
            } catch (error) {
                console.error('Erro ao excluir:', error);
                setMessageModal({
                    isOpen: true,
                    title: 'Erro',
                    message: 'Erro ao excluir registro.',
                    type: 'error'
                });
            }
        });
        setModalOpen(true);
    };

    const executePendingAction = async () => {
        if (pendingAction) {
            await pendingAction();
        }
        setModalOpen(false);
        setPendingAction(null);
    };

    return (
        <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>
            <div style={{
                marginBottom: '20px',
                padding: '15px',
                border: '1px solid #ddd',
                borderRadius: '8px',
                background: '#f9f9f9',
                color: '#333'
            }}>
                <div style={{ marginBottom: '10px', fontWeight: 'bold' }}>Filtros:</div>
                <div style={{ display: 'flex', gap: '30px', alignItems: 'center', flexWrap: 'wrap' }}>
                    <div style={{ flex: 1, minWidth: '200px' }}>
                        <input
                            type="text"
                            name="description"
                            placeholder="Descrição..."
                            value={filters.description}
                            onChange={handleFilterChange}
                            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc' }}
                        />
                    </div>
                    <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                name="types"
                                value="INCOME"
                                checked={filters.types.includes('INCOME')}
                                onChange={handleFilterChange}
                                style={{ marginRight: '8px' }}
                            /> Entrada
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                name="types"
                                value="EXPENSE"
                                checked={filters.types.includes('EXPENSE')}
                                onChange={handleFilterChange}
                                style={{ marginRight: '8px' }}
                            /> Saída
                        </label>
                    </div>
                </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
                <button
                    onClick={() => navigate('/transaction-types/new')}
                    className="btn-primary"
                    style={{ padding: '8px 16px', fontSize: '0.9rem' }}
                >
                    Novo Tipo de Transação
                </button>
            </div>

            <div className="card">
                <table width="100%" style={{ borderCollapse: 'collapse' }}>
                    <thead>
                        <tr style={{ borderBottom: '1px solid #ddd', textAlign: 'left' }}>
                            <th style={{ padding: '10px' }}>Descrição</th>
                            <th style={{ padding: '10px' }}>Tipo</th>
                            <th style={{ padding: '10px' }}>Recorrente</th>
                            <th style={{ padding: '10px', width: '100px' }}>Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="4" style={{ padding: '20px', textAlign: 'center' }}>Carregando...</td></tr>
                        ) : transactionTypes.length === 0 ? (
                            <tr><td colSpan="4" style={{ padding: '20px', textAlign: 'center' }}>Nenhum registro encontrado.</td></tr>
                        ) : (
                            transactionTypes.map(item => (
                                <tr key={item.id} style={{ borderBottom: '1px solid #eee' }}>
                                    <td style={{ padding: '10px' }}>{item.description}</td>
                                    <td style={{ padding: '10px' }}>{item.type === 'INCOME' ? 'Entrada' : 'Saída'}</td>
                                    <td style={{ padding: '10px' }}>{item.recurring ? 'Sim' : 'Não'}</td>
                                    <td style={{ padding: '10px', display: 'flex', gap: '10px' }}>
                                        <button
                                            onClick={() => navigate(`/transaction-types/${item.id}/edit`)}
                                            className="btn-icon"
                                            title="Alterar"
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'orange' }}
                                        >
                                            <FaEdit size={18} />
                                        </button>
                                        <button
                                            onClick={() => handleDeleteClick(item.id)}
                                            className="btn-icon"
                                            title="Excluir"
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'red' }}
                                        >
                                            <FaTrash size={18} />
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>

                {/* Pagination */}
                <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center', gap: '10px' }}>
                    <button
                        disabled={page === 0}
                        onClick={() => setPage(p => p - 1)}
                        className="btn-secondary"
                    >
                        Anterior
                    </button>
                    <span style={{ alignSelf: 'center' }}>Página {page + 1} de {totalPages || 1}</span>
                    <button
                        disabled={page >= totalPages - 1}
                        onClick={() => setPage(p => p + 1)}
                        className="btn-secondary"
                    >
                        Próxima
                    </button>
                </div>
            </div>

            <ConfirmationModal
                isOpen={modalOpen}
                onClose={() => setModalOpen(false)}
                onConfirm={executePendingAction}
                title="Confirmar Exclusão"
                message="Tem certeza que deseja excluir este tipo de transação?"
                confirmText="Sim, excluir"
                cancelText="Cancelar"
            />

            <MessageModal
                isOpen={messageModal.isOpen}
                onClose={() => setMessageModal({ ...messageModal, isOpen: false })}
                title={messageModal.title}
                message={messageModal.message}
                type={messageModal.type}
            />
        </div>
    );
}
