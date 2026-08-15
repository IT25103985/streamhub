import tkinter as tk
from tkinter import scrolledtext
import subprocess
import threading
import os
import signal
import shutil
import webbrowser

class DevManagerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("StreamHub Dev Manager")
        self.root.geometry("900x600")

        # Process handlers
        self.backend_process = None
        self.frontend_process = None

        # Base path
        self.base_path = os.path.dirname(os.path.abspath(__file__))

        self.create_widgets()

    def create_widgets(self):
        # Buttons frame
        btn_frame = tk.Frame(self.root)
        btn_frame.pack(pady=10)

        # Backend Controls
        self.btn_start_backend = tk.Button(btn_frame, text="Start Backend", width=15, command=self.start_backend, bg="#4CAF50", fg="white", font=("Helvetica", 10, "bold"))
        self.btn_start_backend.grid(row=0, column=0, padx=5, pady=5)
        
        self.btn_stop_backend = tk.Button(btn_frame, text="Stop Backend", width=15, state=tk.DISABLED, command=self.stop_backend, bg="#f44336", fg="white", font=("Helvetica", 10, "bold"))
        self.btn_stop_backend.grid(row=0, column=1, padx=5, pady=5)
        
        self.btn_restart_backend = tk.Button(btn_frame, text="Restart Backend", width=15, state=tk.DISABLED, command=self.restart_backend, bg="#FF9800", fg="black", font=("Helvetica", 10, "bold"))
        self.btn_restart_backend.grid(row=0, column=2, padx=5, pady=5)

        self.btn_open_backend = tk.Button(btn_frame, text="Open API", width=12, state=tk.DISABLED, command=lambda: webbrowser.open("http://localhost:8080"), bg="#9C27B0", fg="white", font=("Helvetica", 9, "bold"))
        self.btn_open_backend.grid(row=0, column=3, padx=5, pady=5)

        # Frontend Controls
        self.btn_start_frontend = tk.Button(btn_frame, text="Start Frontend", width=15, command=self.start_frontend, bg="#2196F3", fg="white", font=("Helvetica", 10, "bold"))
        self.btn_start_frontend.grid(row=1, column=0, padx=5, pady=5)
        
        self.btn_stop_frontend = tk.Button(btn_frame, text="Stop Frontend", width=15, state=tk.DISABLED, command=self.stop_frontend, bg="#f44336", fg="white", font=("Helvetica", 10, "bold"))
        self.btn_stop_frontend.grid(row=1, column=1, padx=5, pady=5)
        
        self.btn_restart_frontend = tk.Button(btn_frame, text="Restart Frontend", width=15, state=tk.DISABLED, command=self.restart_frontend, bg="#FF9800", fg="black", font=("Helvetica", 10, "bold"))
        self.btn_restart_frontend.grid(row=1, column=2, padx=5, pady=5)

        self.btn_open_frontend = tk.Button(btn_frame, text="Open App", width=12, state=tk.DISABLED, command=lambda: webbrowser.open("http://localhost:5173"), bg="#9C27B0", fg="white", font=("Helvetica", 9, "bold"))
        self.btn_open_frontend.grid(row=1, column=3, padx=5, pady=5)

        # Stop All
        self.btn_stop_all = tk.Button(btn_frame, text="STOP ALL", width=15, bg="#b71c1c", fg="white", font=("Helvetica", 11, "bold"), command=self.stop_all)
        self.btn_stop_all.grid(row=0, column=4, rowspan=2, padx=20, pady=5, sticky="ns")

        # Output Text
        self.log_area = scrolledtext.ScrolledText(self.root, wrap=tk.WORD, bg="#1e1e1e", fg="#00ff00", font=("Consolas", 10))
        self.log_area.pack(expand=True, fill='both', padx=10, pady=10)
        
        # Configure tags for different colors
        self.log_area.tag_config("BACKEND", foreground="#FFD700") # Gold for backend
        self.log_area.tag_config("FRONTEND", foreground="#00FFFF") # Cyan for frontend
        self.log_area.tag_config("SYSTEM", foreground="#FFFFFF") # White for system messages

        self.log("StreamHub Dev Manager initialized. Ready to start services.", "SYSTEM")
        self.log("Python " + __import__('sys').version.split()[0], "SYSTEM")

    def log(self, message, tag="SYSTEM"):
        self.log_area.config(state=tk.NORMAL)
        self.log_area.insert(tk.END, message + "\n", tag)
        self.log_area.see(tk.END)
        self.log_area.config(state=tk.DISABLED)

    def run_process(self, name, cmd, cwd, set_process_ref):
        tag = name
        self.log(f"[{name}] Starting: {' '.join(cmd)} in {cwd}", tag)
        
        # We use CREATE_NEW_PROCESS_GROUP so we can send CTRL_BREAK_EVENT to kill the process tree on Windows
        try:
            process = subprocess.Popen(
                cmd,
                cwd=cwd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                stdin=subprocess.PIPE,
                text=True,
                creationflags=subprocess.CREATE_NEW_PROCESS_GROUP
            )
            set_process_ref(process)
            
            # Read output in a thread
            def read_output():
                try:
                    for line in iter(process.stdout.readline, ''):
                        if line:
                            # Schedule UI update on main thread
                            self.root.after(0, self.log, f"[{name}] {line.strip()}", tag)
                except Exception as e:
                    self.root.after(0, self.log, f"[{name}] Error reading output: {e}", tag)
                finally:
                    process.stdout.close()
                    process.wait()
                    self.root.after(0, self.log, f"[{name}] Process exited with code {process.returncode}", tag)
                    self.root.after(0, set_process_ref, None)
                
            threading.Thread(target=read_output, daemon=True).start()
        except Exception as e:
            self.log(f"[{name}] Failed to start: {e}", "SYSTEM")

    def set_backend_process(self, proc):
        self.backend_process = proc
        active = proc is not None
        self.btn_start_backend.config(state=tk.DISABLED if active else tk.NORMAL)
        self.btn_stop_backend.config(state=tk.NORMAL if active else tk.DISABLED)
        self.btn_restart_backend.config(state=tk.NORMAL if active else tk.DISABLED)
        self.btn_open_backend.config(state=tk.NORMAL if active else tk.DISABLED)

    def set_frontend_process(self, proc):
        self.frontend_process = proc
        active = proc is not None
        self.btn_start_frontend.config(state=tk.DISABLED if active else tk.NORMAL)
        self.btn_stop_frontend.config(state=tk.NORMAL if active else tk.DISABLED)
        self.btn_restart_frontend.config(state=tk.NORMAL if active else tk.DISABLED)
        self.btn_open_frontend.config(state=tk.NORMAL if active else tk.DISABLED)

    def start_backend(self):
        cwd = os.path.join(self.base_path, "backend")
        cmd = [os.path.join(cwd, "mvnw.cmd"), "spring-boot:run"]
        self.run_process("BACKEND", cmd, cwd, self.set_backend_process)

    def stop_backend(self, callback=None):
        if self.backend_process:
            self.log("[BACKEND] Stopping...", "BACKEND")
            try:
                self.backend_process.send_signal(signal.CTRL_BREAK_EVENT)
            except Exception as e:
                self.log(f"[BACKEND] Error sending stop signal: {e}", "SYSTEM")
            
            def wait_and_call():
                try:
                    self.backend_process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.backend_process.kill() # Force kill if not stopping
                
                if callback:
                    self.root.after(500, callback)
            threading.Thread(target=wait_and_call, daemon=True).start()
        elif callback:
            callback()

    def restart_backend(self):
        self.stop_backend(callback=self.start_backend)

    def start_frontend(self):
        cwd = os.path.join(self.base_path, "frontend")
        npm_path = shutil.which("npm") or "npm.cmd"
        cmd = [npm_path, "run", "dev"]
        self.run_process("FRONTEND", cmd, cwd, self.set_frontend_process)

    def stop_frontend(self, callback=None):
        if self.frontend_process:
            self.log("[FRONTEND] Stopping...", "FRONTEND")
            try:
                self.frontend_process.send_signal(signal.CTRL_BREAK_EVENT)
            except Exception as e:
                self.log(f"[FRONTEND] Error sending stop signal: {e}", "SYSTEM")
            
            def wait_and_call():
                try:
                    self.frontend_process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self.frontend_process.kill()
                    
                if callback:
                    self.root.after(500, callback)
            threading.Thread(target=wait_and_call, daemon=True).start()
        elif callback:
            callback()

    def restart_frontend(self):
        self.stop_frontend(callback=self.start_frontend)

    def stop_all(self):
        self.stop_backend()
        self.stop_frontend()
        self.log("Sent stop signals to all processes.", "SYSTEM")

    def on_closing(self):
        self.stop_all()
        # Wait a bit for processes to die then destroy
        self.root.after(1000, self.root.destroy)

if __name__ == "__main__":
    root = tk.Tk()
    app = DevManagerApp(root)
    root.protocol("WM_DELETE_WINDOW", app.on_closing)
    root.mainloop()
