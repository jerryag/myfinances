import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Home } from './pages/Home';
import { ChangePassword } from './pages/ChangePassword';
import { UserList } from './pages/UserList';
import { UserForm } from './pages/UserForm';
import { TransactionTypeList } from './pages/TransactionTypeList';
import { TransactionTypeForm } from './pages/TransactionTypeForm';

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Login />} />

                    <Route element={
                        <ProtectedRoute>
                            <Layout />
                        </ProtectedRoute>
                    }>
                        <Route path="/home" element={<Home />} />
                        <Route path="/change-password" element={<ChangePassword />} />

                        <Route path="/transaction-types" element={<TransactionTypeList />} />
                        <Route path="/transaction-types/new" element={<TransactionTypeForm />} />
                        <Route path="/transaction-types/:id/edit" element={<TransactionTypeForm />} />

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
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;
