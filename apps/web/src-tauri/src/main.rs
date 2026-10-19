// Sem janela de console no executável de release do Windows
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
  brewapp_desktop_lib::run();
}
