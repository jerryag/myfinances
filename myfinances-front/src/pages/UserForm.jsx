import { useState, useEffect } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { userService } from '../services/api';

export const UserForm = () => {
    const navigate = useNavigate();
    const { id } = useParams();
    const location = useLocation();
    const isEdit = !!id;

    const [formData, setFormData] = useState({
        name: '',
        login: '',
        password: '',
        type: 'USER',
        status: 'ACTIVE'
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (isEdit) {
            if (location.state && location.state.user) {
                const user = location.state.user;
                setFormData({
                    name: user.name,
                    login: user.login,
                    type: user.type,
                    status: user.status || 'ACTIVE'
                });
            } else {
                // If checking via URL directly
                navigate('/users');
            }
        }
    }, [isEdit, location.state, navigate]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            if (isEdit) {
                await userService.update(id, formData);
            } else {
                await userService.create(formData);
            }
            navigate('/users');
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Erro ao salvar usuário.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card" style={{ maxWidth: '600px' }}>
                <h2>{isEdit ? 'Editar Usuário' : 'Novo Usuário'}</h2>
                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Nome</label>
                        <input
                            type="text"
                            value={formData.name}
                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Login (E-mail)</label>
                        <input
                            type="email"
                            value={formData.login}
                            onChange={(e) => setFormData({ ...formData, login: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Tipo</label>
                        <select
                            value={formData.type}
                            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                            style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ccc' }}
                        >
                            <option value="USER">Usuário</option>
                            <option value="ADMIN">Administrador</option>
                        </select>
                    </div>
                    <div className="form-group">
                        <label>{isEdit ? 'Nova Senha (Opcional)' : 'Senha Provisória (Opcional)'}</label>
                        <input
                            type="password"
                            value={formData.password}
                            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                            placeholder={isEdit ? "Deixe em branco para manter a atual" : "Padrão: MyFinances@123"}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                        <button type="submit" className="btn-primary" disabled={loading}>
                            {loading ? 'Salvando...' : 'Salvar'}
                        </button>
                        <button type="button" onClick={() => navigate('/users')} className="btn-secondary" style={{ background: '#ccc', border: 'none', padding: '0.75rem', borderRadius: '4px', cursor: 'pointer' }}>
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
