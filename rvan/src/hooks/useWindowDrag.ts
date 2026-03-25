import { useEffect } from 'react';
import { invoke } from '@tauri-apps/api/core';

/**
 * Hook to enable native window dragging on a DOM element.
 * Uses Tauri's `start_dragging` command.
 *
 * @param ref - Ref to the element that should trigger drag (e.g., title bar area)
 */
export function useWindowDrag(ref: React.RefObject<HTMLElement>) {
  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    const handleMouseDown = () => {
      // Trigger native window drag via Tauri
      invoke('start_dragging').catch(console.error);
    };

    element.addEventListener('mousedown', handleMouseDown);
    return () => {
      element.removeEventListener('mousedown', handleMouseDown);
    };
  }, []);
}