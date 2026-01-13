import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './pages/Login';
import { Home } from './pages/Home';
import { ChangePassword } from './pages/ChangePassword';
import { UserList } from './pages/UserList';
import { UserForm } from './pages/UserForm';

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Login />} />
                    <Route path="/change-password" element={
                        <ProtectedRoute>
                            <ChangePassword />
                        </ProtectedRoute>
                    } />
                    <Route path="/home" element={
                        <ProtectedRoute>
                            <Home />
                        </ProtectedRoute>
                    } />
                    <Route path="/users" element={
                        <ProtectedRoute roles={['ADMIN', 'MASTER']}>
                            <UserList />
                        </ProtectedRoute>
                    } />
                    <Route path="/users/new" element={
                        <ProtectedRoute roles={['ADMIN', 'MASTER']}>
                            <UserForm />
                        </ProtectedRoute>
                    } />
                    <Route path="/users/:id/edit" element={
                        <ProtectedRoute roles={['ADMIN', 'MASTER']}>
                            <UserForm />
                        </ProtectedRoute>
                    } />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;
