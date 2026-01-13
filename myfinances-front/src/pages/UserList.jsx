import { useState, useEffect } from 'react';
import { userService } from '../services/api';
import { useNavigate } from 'react-router-dom';

export const UserList = () => {
    const navigate = useNavigate();
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [filters, setFilters] = useState({
        ACTIVE: false,
        BLOCKED: false,
        DELETED: false
    });

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

    const handleStatusChange = async (id, newStatus) => {
        if (!confirm(`Deseja realmente alterar o status para ${newStatus}?`)) return;
        try {
            await userService.changeStatus(id, newStatus);
            fetchUsers();
        } catch (error) {
            console.error('Erro ao alterar status', error);
            alert('Erro ao alterar status');
        }
    };

    const handleEdit = (user) => {
        // We will implement navigation to edit later, or use a modal.
        // For now, let's assume a modal or a separate route.
        // Given the prompt "create a new user... form of creation", let's use a separate route or modal.
        // Let's use navigation to /users/edit/:id or /users/new
        // Since I haven't created the form yet, I'll just log or alert for now.
        // Ideally, navigate(`/users/${user.id}`);
        // But let's build the UserForm first. For now, placeholder.
        navigate(`/users/${user.id}`);
    };

    return (
        <div style={{ padding: '20px' }}>
            <h2>Gerenciamento de Usuários</h2>

            <div style={{ marginBottom: '20px', display: 'flex', gap: '20px', alignItems: 'center' }}>
                <div>
                    <strong>Filtros:</strong>
                    <label style={{ marginLeft: '10px' }}>
                        <input type="checkbox" checked={filters.ACTIVE} onChange={() => handleFilterChange('ACTIVE')} /> Ativos
                    </label>
                    <label style={{ marginLeft: '10px' }}>
                        <input type="checkbox" checked={filters.BLOCKED} onChange={() => handleFilterChange('BLOCKED')} /> Bloqueados
                    </label>
                    <label style={{ marginLeft: '10px' }}>
                        <input type="checkbox" checked={filters.DELETED} onChange={() => handleFilterChange('DELETED')} /> Excluídos
                    </label>
                </div>
                <button onClick={() => navigate('/users/new')} className="btn-primary">Novo Usuário</button>
            </div>

            {loading ? <p>Carregando...</p> : (
                <>
                    <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }} border="1">
                        <thead>
                            <tr>
                                <th>Login</th>
                                <th>Nome</th>
                                <th>Tipo</th>
                                <th>Status</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map(user => (
                                <tr key={user.id}>
                                    <td>{user.login}</td>
                                    <td>{user.name}</td>
                                    <td>{
                                        {
                                            'ADMIN': 'Administrador',
                                            'USER': 'Usuário',
                                            'MASTER': 'Master'
                                        }[user.type] || user.type
                                    }</td>
                                    <td>{
                                        {
                                            'ACTIVE': 'Ativo',
                                            'BLOCKED': 'Bloqueado',
                                            'DELETED': 'Deletado'
                                        }[user.status || 'ACTIVE'] || user.status
                                    }</td>
                                    <td>
                                        <div style={{ display: 'flex', gap: '5px' }}>
                                            {user.status !== 'DELETED' && (
                                                <button
                                                    onClick={() => navigate(`/users/${user.id}/edit`, { state: { user } })}
                                                    className="btn-secondary"
                                                    style={{ padding: '5px 10px', fontSize: '0.8rem' }}
                                                >
                                                    Alterar
                                                </button>
                                            )}

                                            {user.status === 'ACTIVE' && (
                                                <button
                                                    onClick={() => handleStatusChange(user.id, 'BLOCKED')}
                                                    style={{ background: '#f0ad4e', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' }}
                                                >
                                                    Bloquear
                                                </button>
                                            )}

                                            {user.status === 'BLOCKED' && (
                                                <button
                                                    onClick={() => handleStatusChange(user.id, 'ACTIVE')}
                                                    style={{ background: '#5bc0de', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' }}
                                                >
                                                    Desbloquear
                                                </button>
                                            )}

                                            {user.status !== 'DELETED' && (
                                                <button
                                                    onClick={() => handleStatusChange(user.id, 'DELETED')}
                                                    style={{ background: '#d9534f', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem' }}
                                                >
                                                    Excluir
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div style={{ marginTop: '20px' }}>
                        <button disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button>
                        <span style={{ margin: '0 10px' }}>Página {page + 1} de {totalPages}</span>
                        <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Próxima</button>
                    </div>
                </>
            )}
        </div>
    );
};
