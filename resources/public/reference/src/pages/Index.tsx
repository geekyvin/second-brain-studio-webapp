
import { useState } from "react";
import Sidebar from "../components/Sidebar";
import ActionBar from "../components/ActionBar";
import NoteEditor from "../components/NoteEditor";

const Index = () => {
  const [sidebarMinimized, setSidebarMinimized] = useState(false);
  
  const toggleSidebar = () => {
    setSidebarMinimized(!sidebarMinimized);
  };
  
  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <Sidebar 
        minimized={sidebarMinimized} 
        onToggle={toggleSidebar} 
      />
      
      <div className="flex-1 flex flex-col overflow-hidden">
        <ActionBar />
        
        <main className="flex-1 overflow-auto bg-white">
          <NoteEditor />
        </main>
      </div>
    </div>
  );
};

export default Index;
