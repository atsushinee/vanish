import { useEffect } from 'react';
import { getCurrentWindow } from '@tauri-apps/api/window';

/**
 * Hook to enable native window dragging on a DOM element.
 * Uses Tauri window API `startDragging`.
 *
 * @param ref - Ref to the element that should trigger drag (e.g., title bar area)
 */
export function useWindowDrag(ref: React.RefObject<HTMLElement | null>) {
  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const handleMouseDown = () => {
      getCurrentWindow().startDragging().catch(console.error);
    };

    element.addEventListener('mousedown', handleMouseDown);
    return () => {
      element.removeEventListener('mousedown', handleMouseDown);
    };
  }, [ref]);
}
