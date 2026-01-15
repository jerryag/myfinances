import { useState, useEffect } from 'react';
import { userService } from '../services/api';
import { useNavigate } from 'react-router-dom';
import { FaEdit, FaTrash, FaLock, FaUnlock } from 'react-icons/fa';

import { usePageTitle } from '../context/PageTitleContext';
import { ConfirmationModal } from '../components/ConfirmationModal';

export const UserList = () => {
    const navigate = useNavigate();
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filters, setFilters] = useState({
        ACTIVE: true,
        BLOCKED: true,
        DELETED: false
    });

    // Modal State
    const [modalOpen, setModalOpen] = useState(false);
    const [modalConfig, setModalConfig] = useState({
        title: '',
        message: '',
        onConfirm: null
    });

    const openModal = (title, message, onConfirm) => {
        setModalConfig({ title, message, onConfirm });
        setModalOpen(true);
    };

    const confirmAction = () => {
        if (modalConfig.onConfirm) {
            modalConfig.onConfirm();
        }
        setModalOpen(false);
    };

    usePageTitle('Gerenciamento de Usuários');

    const fetchUsers = async () => {
        setLoading(true);
        try {
            const selectedStatuses = Object.keys(filters).filter(key => filters[key]);
            const params = {
                page,
                size: 10,
                sort: 'login,asc',
            };

            if (selectedStatuses.length > 0) {
                params.statuses = selectedStatuses.join(',');
            }

            const response = await userService.getAll(params);
            setUsers(response.data.content);
            setTotalPages(response.data.totalPages);
        } catch (error) {
            console.error('Erro ao buscar usuários', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, [page, filters]);

    const handleFilterChange = (status) => {
        setFilters(prev => ({ ...prev, [status]: !prev[status] }));
        setPage(0); // Reset to first page on filter change
    };

    const handleStatusChange = (id, newStatus) => {
        const action = async () => {
            try {
                await userService.changeStatus(id, newStatus);
                fetchUsers();
            } catch (error) {
                console.error('Erro ao alterar status', error);
                alert('Erro ao alterar status');
            }
        };

        const statusMap = {
            'ACTIVE': 'Desbloquear',
            'BLOCKED': 'Bloquear',
            'DELETED': 'Excluir'
        };

        openModal(
            'Confirmação',
            `Deseja realmente ${statusMap[newStatus].toLowerCase()} este usuário?`,
            action
        );
    };

    return (
        <div style={{ padding: '20px', maxWidth: '1000px', margin: '0 auto' }}>

            <div style={{
                marginBottom: '20px',
                padding: '15px',
                border: '1px solid #ddd',
                borderRadius: '8px',
                background: '#f9f9f9',
                color: '#333' // Fix contrast
            }}>
                <div style={{ marginBottom: '10px', fontWeight: 'bold' }}>Filtros:</div>
                <div style={{ display: 'flex', gap: '20px' }}>
                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                        <input type="checkbox" checked={filters.ACTIVE} onChange={() => handleFilterChange('ACTIVE')} style={{ marginRight: '8px' }} /> Ativos
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                        <input type="checkbox" checked={filters.BLOCKED} onChange={() => handleFilterChange('BLOCKED')} style={{ marginRight: '8px' }} /> Bloqueados
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                        <input type="checkbox" checked={filters.DELETED} onChange={() => handleFilterChange('DELETED')} style={{ marginRight: '8px' }} /> Excluídos
                    </label>
                </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '20px' }}>
                <button
                    onClick={() => navigate('/users/new')}
                    className="btn-primary"
                    style={{ padding: '8px 16px', fontSize: '0.9rem' }}
                >
                    Novo Usuário
                </button>
            </div>

            {loading ? <p>Carregando...</p> : (
                <>
                    <div style={{ overflowX: 'auto', border: '1px solid #eee', borderRadius: '4px' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead style={{ background: '#f5f5f5', color: '#333' }}>
                                <tr>
                                    <th style={{ padding: '12px 15px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Login</th>
                                    <th style={{ padding: '12px 15px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Nome</th>
                                    <th style={{ padding: '12px 15px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Tipo</th>
                                    <th style={{ padding: '12px 15px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Status</th>
                                    <th style={{ padding: '12px 15px', textAlign: 'left', borderBottom: '1px solid #ddd' }}>Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(user => (
                                    <tr key={user.id} style={{ borderBottom: '1px solid #eee' }}>
                                        <td style={{ padding: '10px 15px' }}>{user.login}</td>
                                        <td style={{ padding: '10px 15px' }}>{user.name}</td>
                                        <td style={{ padding: '10px 15px' }}>{
                                            {
                                                'ADMIN': 'Administrador',
                                                'USER': 'Usuário',
                                                'MASTER': 'Master'
                                            }[user.type] || user.type
                                        }</td>
                                        <td style={{ padding: '10px 15px' }}>{
                                            {
                                                'ACTIVE': 'Ativo',
                                                'BLOCKED': 'Bloqueado',
                                                'DELETED': 'Excluído'
                                            }[user.status || 'ACTIVE'] || user.status
                                        }</td>
                                        <td style={{ padding: '10px 15px' }}>
                                            <div style={{ display: 'flex', gap: '8px' }}>
                                                {user.status !== 'DELETED' && (
                                                    <button
                                                        onClick={() => navigate(`/users/${user.id}/edit`, { state: { user } })}
                                                        title="Editar"
                                                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#337ab7', fontSize: '1.1rem' }}
                                                    >
                                                        <FaEdit />
                                                    </button>
                                                )}

                                                {user.status === 'ACTIVE' && (
                                                    <button
                                                        onClick={() => handleStatusChange(user.id, 'BLOCKED')}
                                                        title="Bloquear"
                                                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#f0ad4e', fontSize: '1.1rem' }}
                                                    >
                                                        <FaLock />
                                                    </button>
                                                )}

                                                {user.status === 'BLOCKED' && (
                                                    <button
                                                        onClick={() => handleStatusChange(user.id, 'ACTIVE')}
                                                        title="Desbloquear"
                                                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#5bc0de', fontSize: '1.1rem' }}
                                                    >
                                                        <FaUnlock />
                                                    </button>
                                                )}

                                                {user.status !== 'DELETED' && (
                                                    <button
                                                        onClick={() => handleStatusChange(user.id, 'DELETED')}
                                                        title="Excluir"
                                                        style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#d9534f', fontSize: '1.1rem' }}
                                                    >
                                                        <FaTrash />
                                                    </button>
                                                )}
                                            </div>
                                        </td>

                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center', gap: '10px' }}>
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(page - 1)}
                            style={{
                                padding: '8px 12px',
                                background: page === 0 ? '#ccc' : '#007bff',
                                color: '#333',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: page === 0 ? 'not-allowed' : 'pointer'
                            }}
                        >
                            Anterior
                        </button>
                        <span style={{ display: 'flex', alignItems: 'center' }}>Página {page + 1} de {totalPages}</span>
                        <button
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(page + 1)}
                            style={{
                                padding: '8px 12px',
                                background: page >= totalPages - 1 ? '#ccc' : '#007bff',
                                color: '#333',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer'
                            }}
                        >
                            Próxima
                        </button>
                    </div>
                </>
            )}
            {/* Render Modal */}
            <ConfirmationModal
                isOpen={modalOpen}
                onClose={() => setModalOpen(false)}
                onConfirm={confirmAction}
                title={modalConfig.title}
                message={modalConfig.message}
            />
        </div>
    );
};
