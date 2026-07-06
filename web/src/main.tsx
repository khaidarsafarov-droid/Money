import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { DashboardPage } from "@/pages/Dashboard";
import { ToastProvider } from "@/components/ui/Toast";
import "@/index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ToastProvider>
      <DashboardPage />
    </ToastProvider>
  </StrictMode>,
);
