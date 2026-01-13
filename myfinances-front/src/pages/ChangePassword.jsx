import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

export const ChangePassword = () => {
    const { user, changePassword, logout } = useAuth();
    const navigate = useNavigate();

    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [validations, setValidations] = useState({
        length: false,
        upper: false,
        lower: false,
        number: false,
        symbol: false
    });

    useEffect(() => {
        if (!user) {
            navigate('/');
        }
    }, [user, navigate]);

    useEffect(() => {
        setValidations({
            length: newPassword.length >= 8,
            upper: /[A-Z]/.test(newPassword),
            lower: /[a-z]/.test(newPassword),
            number: /[0-9]/.test(newPassword),
            symbol: /[^a-zA-Z0-9]/.test(newPassword)
        });
    }, [newPassword]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (newPassword !== confirmPassword) {
            setError('A nova senha e a confirmação não conferem.');
            return;
        }

        if (!Object.values(validations).every(Boolean)) {
            setError('A nova senha não atende a todos os requisitos.');
            return;
        }

        try {
            await changePassword(user.id, oldPassword, newPassword);
            // Updating user state locally to remove the flag would be ideal, 
            // but for now redirecting to home assuming the backend updated the user. 
            // Better UX might be to logout or force refresh user data.
            // Given the requirements "redirect to Home":

            // We need to update the user object in context/localStorage because 
            // it still has changePwdOnLogin=true. 
            // Simplest way: manually update localStorage and force a reload or 
            // update context. Since context update is not exposed, we might just 
            // navigate to home, but if Home checks the flag again it might loop.
            // Let's assume Home doesn't check, only Login does.

            // To be safe, update localStorage user
            const updatedUser = { ...user, changePwdOnLogin: false };
            localStorage.setItem('user', JSON.stringify(updatedUser));

            navigate('/home');
            window.location.reload(); // Reload to ensure context picks up the new user state if needed
        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || 'Erro ao alterar a senha.');
        }
    };

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <div className="login-container">
            <div className="login-card" style={{ maxWidth: '500px' }}>
                <h2>Alterar Senha</h2>
                <p>Você precisa alterar sua senha para continuar.</p>

                {error && <div className="error-message">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Senha Atual</label>
                        <input
                            type="password"
                            value={oldPassword}
                            onChange={(e) => setOldPassword(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Nova Senha</label>
                        <input
                            type="password"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                        />
                    </div>

                    <div className="password-requirements" style={{ fontSize: '0.85rem', color: '#666', marginBottom: '1rem' }}>
                        Requisitos:
                        <ul style={{ listStyle: 'none', paddingLeft: 0 }}>
                            <li style={{ color: validations.length ? 'green' : 'red' }}>
                                {validations.length ? '✓' : '✗'} Mínimo 8 caracteres
                            </li>
                            <li style={{ color: validations.upper ? 'green' : 'red' }}>
                                {validations.upper ? '✓' : '✗'} Pelo menos uma letra maiúscula
                            </li>
                            <li style={{ color: validations.lower ? 'green' : 'red' }}>
                                {validations.lower ? '✓' : '✗'} Pelo menos uma letra minúscula
                            </li>
                            <li style={{ color: validations.number ? 'green' : 'red' }}>
                                {validations.number ? '✓' : '✗'} Pelo menos um número
                            </li>
                            <li style={{ color: validations.symbol ? 'green' : 'red' }}>
                                {validations.symbol ? '✓' : '✗'} Pelo menos um símbolo
                            </li>
                        </ul>
                    </div>

                    <div className="form-group">
                        <label>Confirmar Nova Senha</label>
                        <input
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                        <button type="submit" className="btn-primary" disabled={!Object.values(validations).every(Boolean)}>
                            Alterar Senha
                        </button>
                        <button type="button" onClick={handleLogout} className="btn-secondary" style={{ background: '#ccc', border: 'none', padding: '0.75rem', borderRadius: '4px', cursor: 'pointer' }}>
                            Cancelar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
