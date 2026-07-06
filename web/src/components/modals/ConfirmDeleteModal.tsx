import { Modal, ModalButton } from "@/components/ui/Modal";

interface ConfirmDeleteModalProps {
  open: boolean;
  title: string;
  message: string;
  onClose: () => void;
  onConfirm: () => void;
}

export function ConfirmDeleteModal({
  open,
  title,
  message,
  onClose,
  onConfirm,
}: ConfirmDeleteModalProps) {
  return (
    <Modal
      open={open}
      title={title}
      onClose={onClose}
      footer={
        <>
          <ModalButton variant="secondary" onClick={onClose}>
            Отмена
          </ModalButton>
          <ModalButton
            variant="danger"
            onClick={() => {
              onConfirm();
              onClose();
            }}
          >
            Удалить
          </ModalButton>
        </>
      }
    >
      <p className="text-sm text-milk-muted">{message}</p>
    </Modal>
  );
}
